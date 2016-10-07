package se.svt.core.lib.utils.rest;


import se.svt.core.lib.utils.rest.retry.RetryableException;
import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;
import se.svt.core.lib.utils.rest.util.MethodUtils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Throwables.getRootCause;

@RequiredArgsConstructor
class SyncRequestHelper {

    private final RestClientSpecification specification;
    private final RestTemplate restTemplate;

    @Setter
    private boolean retryEnabled;

    Object executeRequest(MethodInvocation invocation, RequestEntity<Object> requestEntity) {
        try {
            return executeRequestInternal(invocation, requestEntity);
        } catch (HttpStatusCodeException ex) {
            return handleHttpStatusCodeException(invocation.getMethod(), ex);
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

    private Object executeRequestInternal(MethodInvocation invocation, RequestEntity<Object> requestEntity) {
        Method method = invocation.getMethod();
        if (MethodUtils.returnTypeIsAnyOf(method, HttpEntity.class, ResponseEntity.class)) {
            return exchangeForResponseEntity(method, requestEntity);
        } else if (MethodUtils.returnTypeIsGeneric(method)) {
            SyntheticParametrizedTypeReference<?> responseType = SyntheticParametrizedTypeReference.fromMethodReturnType(method);
            return extractBodyNullSafe(restTemplate.exchange(requestEntity, responseType));
        }
        Class<?> returnType = GenericTypeResolver.resolveReturnTypeForGenericMethod(method, invocation.getArguments(), null);
        return extractBodyNullSafe(restTemplate.exchange(requestEntity, returnType));
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
