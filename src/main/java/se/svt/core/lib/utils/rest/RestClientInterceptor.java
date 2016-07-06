package se.svt.core.lib.utils.rest;

import se.svt.core.lib.utils.rest.retry.RetryableException;
import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;
import se.svt.core.lib.utils.rest.util.ClassTypeInformation;
import se.svt.core.lib.utils.rest.util.OptionalTypeFutureAdapter;
import se.svt.core.lib.utils.rest.util.ResponseFutureAdapter;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.Objects;
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

        if (isListenableFuture(method)) {
            return asyncRequest(method, requestEntity);
        }
        return syncRequest(method, requestEntity);
    }

    private Object asyncRequest(Method method, RequestEntity<Object> requestEntity) {
        SyntheticParametrizedTypeReference<?> responseType = SyntheticParametrizedTypeReference.fromAsyncMethodReturnType(method);
        ListenableFuture<? extends ResponseEntity<?>> listenableFuture = sendAsyncRequest(requestEntity, responseType);
        if (responseType.isOptionalType()) {
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

    private static boolean isListenableFuture(Method method) {
        Class<?> clazz = ClassTypeInformation.fromReturnTypeOf(method).getType();
        return Objects.equals(ListenableFuture.class, clazz);
    }

    private Object syncRequest(Method method, RequestEntity<Object> requestEntity) {
        ResponseEntity<?> responseEntity;
        try {
            if (method.getGenericReturnType() instanceof ParameterizedType) {
                SyntheticParametrizedTypeReference<?> responseType = SyntheticParametrizedTypeReference.fromMethodReturnType(method);
                responseEntity = restTemplate.exchange(requestEntity, responseType);
            } else {
                responseEntity = restTemplate.exchange(requestEntity, method.getReturnType());
            }
            return Optional.ofNullable(responseEntity).map(ResponseEntity::getBody).orElse(null);
        } catch (HttpStatusCodeException ex) {
            HttpStatus statusCode = ex.getStatusCode();

            if (isOptional(method) && statusCode.equals(HttpStatus.NOT_FOUND)) {
                return Optional.empty();
            }

            if (retryEnabled && anyMatch(specification.getRetryableStatuses(), statusCode::equals)) {
                throw new RetryableException(ex);
            }
            throw ex;
        } catch (Exception ex) {
            if (retryEnabled && anyMatch(specification.getRetryableExceptions(), clazz -> clazz.isInstance(ex) || clazz.isInstance(getRootCause(ex)))) {
                throw new RetryableException(ex);
            }
            throw ex;
        }
    }

    private static boolean isOptional(Method method) {
        return Objects.equals(method.getReturnType(), Optional.class);
    }

    private static <T> boolean anyMatch(T[] array, Predicate<T> predicate) {
        return Stream.of(array).anyMatch(predicate);
    }

}
