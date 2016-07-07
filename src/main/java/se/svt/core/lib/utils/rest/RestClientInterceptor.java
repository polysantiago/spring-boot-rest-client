package se.svt.core.lib.utils.rest;

import se.svt.core.lib.utils.rest.retry.RetryableException;
import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;
import se.svt.core.lib.utils.rest.util.MethodUtils;
import se.svt.core.lib.utils.rest.util.OptionalTypeFutureAdapter;
import se.svt.core.lib.utils.rest.util.ResponseFutureAdapter;
import se.svt.core.lib.utils.rest.util.TypeUtils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Throwables.getRootCause;

@RequiredArgsConstructor
class RestClientInterceptor implements MethodInterceptor {

    private final RestClientSpecification specification;
    private final RestTemplate restTemplate;
    private final AsyncRestTemplate asyncRestTemplate;
    private final URI serviceUrl;

    @Setter
    private boolean retryEnabled;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        RequestEntity<Object> requestEntity = RestClientInterceptorHelper.from(methodInvocation).buildRequest(serviceUrl);

        if (MethodUtils.returnTypeIs(method, ListenableFuture.class)) {
            return asyncRequest(method, requestEntity);
        }
        return syncRequest(method, requestEntity);
    }

    private Object asyncRequest(Method method, RequestEntity<Object> requestEntity) {
        SyntheticParametrizedTypeReference<?> returnType = SyntheticParametrizedTypeReference
            .fromMethodReturnType(method).getTypeArgument(0);
        if (TypeUtils.typeIsAnyOf(returnType, HttpEntity.class, ResponseEntity.class)) {
            SyntheticParametrizedTypeReference<?> responseType = returnType.getTypeArgument(0);
            return sendAsyncRequest(requestEntity, responseType);
        }
        ListenableFuture<? extends ResponseEntity<?>> listenableFuture = sendAsyncRequest(requestEntity, returnType);
        if (TypeUtils.typeIs(returnType, Optional.class)) {
            return createOptionalTypeAdapter(listenableFuture);
        }
        return createObjectAdapter(listenableFuture);
    }

    private ListenableFuture<? extends ResponseEntity<?>> sendAsyncRequest(RequestEntity<Object> requestEntity,
                                                                           SyntheticParametrizedTypeReference<?> responseType) {
        return asyncRestTemplate.exchange(requestEntity.getUrl(), requestEntity.getMethod(), requestEntity, responseType);
    }

    @SuppressWarnings("unchecked")
    private static OptionalTypeFutureAdapter<Optional<?>> createOptionalTypeAdapter(ListenableFuture<? extends ResponseEntity<?>> listenableFuture) {
        return new OptionalTypeFutureAdapter<>((ListenableFuture<ResponseEntity<Optional<?>>>) listenableFuture);
    }

    @SuppressWarnings("unchecked")
    private static ResponseFutureAdapter<Object> createObjectAdapter(ListenableFuture<? extends ResponseEntity<?>> listenableFuture) {
        return new ResponseFutureAdapter<>((ListenableFuture<ResponseEntity<Object>>) listenableFuture);
    }

    private Object syncRequest(Method method, RequestEntity<Object> requestEntity) {
        try {
            return executeSyncRequest(method, requestEntity);
        } catch (HttpStatusCodeException ex) {
            return handleHttpStatusCodeException(method, ex);
        } catch (RuntimeException ex) {
            throw handleRuntimeException(ex);
        }
    }

    private RuntimeException handleRuntimeException(RuntimeException ex) {
        if (retryEnabled && anyMatch(specification.getRetryableExceptions(), clazz -> clazz.isInstance(ex) || clazz.isInstance(getRootCause(ex)))) {
            return new RetryableException(ex);
        }
        return ex;
    }

    private Object handleHttpStatusCodeException(Method method, HttpStatusCodeException ex) {
        HttpStatus statusCode = ex.getStatusCode();
        if (MethodUtils.returnTypeIs(method, Optional.class) && statusCode.equals(HttpStatus.NOT_FOUND)) {
            return Optional.empty();
        }
        if (retryEnabled && anyMatch(specification.getRetryableStatuses(), statusCode::equals)) {
            throw new RetryableException(ex);
        }
        throw ex;
    }


    private Object executeSyncRequest(Method method, RequestEntity<Object> requestEntity) {
        if (MethodUtils.returnTypeIsAnyOf(method, HttpEntity.class, ResponseEntity.class)) {
            return exchangeForResponseEntity(method, requestEntity);
        } else if (MethodUtils.returnTypeIsGeneric(method)) {
            SyntheticParametrizedTypeReference<?> responseType = SyntheticParametrizedTypeReference.fromMethodReturnType(method);
            return extractBodyNullSafe(restTemplate.exchange(requestEntity, responseType));
        }
        return extractBodyNullSafe(restTemplate.exchange(requestEntity, method.getReturnType()));
    }

    private ResponseEntity<?> exchangeForResponseEntity(Method method, RequestEntity<Object> requestEntity) {
        SyntheticParametrizedTypeReference<?> responseType = SyntheticParametrizedTypeReference
            .fromMethodReturnType(method).getTypeArgument(0);
        return restTemplate.exchange(requestEntity, responseType);
    }

    private Object extractBodyNullSafe(ResponseEntity<?> responseEntity) {
        return Optional.ofNullable(responseEntity).map(ResponseEntity::getBody).orElse(null);
    }

    private static <T> boolean anyMatch(T[] array, Predicate<T> predicate) {
        return Stream.of(array).anyMatch(predicate);
    }

}
