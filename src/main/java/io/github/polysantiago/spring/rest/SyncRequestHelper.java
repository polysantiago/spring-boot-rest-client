package io.github.polysantiago.spring.rest;


import io.github.polysantiago.spring.rest.retry.RetryableException;
import io.github.polysantiago.spring.rest.support.SyntheticParametrizedTypeReference;
import io.github.polysantiago.spring.rest.util.ResolvableTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

@RequiredArgsConstructor
class SyncRequestHelper {

    private final RestClientSpecification specification;
    private final RestTemplate restTemplate;
    private final Class<?> implementingClass;

    @Setter
    private boolean retryEnabled;

    <T> Object executeRequest(MethodInvocation invocation, RequestEntity<T> requestEntity) {
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

    private <T> Optional<T> handleHttpStatusCodeException(Method method, HttpStatusCodeException ex) {
        HttpStatus statusCode = ex.getStatusCode();
        if (ResolvableTypeUtils.returnTypeIs(method, Optional.class) && statusCode.equals(HttpStatus.NOT_FOUND)) {
            return Optional.empty();
        }
        if (retryEnabled && anyMatch(specification.getRetryableStatuses(), statusCode::equals)) {
            throw new RetryableException(ex);
        }
        throw ex;
    }

    private <T> Object executeRequestInternal(MethodInvocation invocation, RequestEntity<T> requestEntity) {
        Method method = invocation.getMethod();
        if (hasPostLocation(method)) {
            return postForLocation(requestEntity, method);
        }
        ResolvableType resolvedType = ResolvableType.forMethodReturnType(method, implementingClass);

        if (ResolvableTypeUtils.returnTypeIsAnyOf(method, HttpEntity.class, ResponseEntity.class)) {
            return exchangeForResponseEntity(resolvedType, requestEntity);
        }
        SyntheticParametrizedTypeReference<T> responseType = SyntheticParametrizedTypeReference.fromResolvableType(resolvedType);
        return extractBodyNullSafe(restTemplate.exchange(requestEntity, responseType));
    }

    private boolean hasPostLocation(Method method) {
        return AnnotationUtils.findAnnotation(method, PostForLocation.class) != null;
    }

    private <T> URI postForLocation(RequestEntity<T> requestEntity, Method method) {
        if (!ResolvableTypeUtils.returnTypeIs(method, URI.class)) {
            throw new RuntimeException("Method annotated with @PostForLocation must return URI");
        }
        return restTemplate.exchange(requestEntity, Object.class).getHeaders().getLocation();
    }

    private <T> ResponseEntity<T> exchangeForResponseEntity(ResolvableType resolvedType, RequestEntity<T> requestEntity) {
        return restTemplate.exchange(requestEntity, SyntheticParametrizedTypeReference.fromResolvableType(resolvedType.getGeneric(0)));
    }

    private <T> T extractBodyNullSafe(ResponseEntity<T> responseEntity) {
        return Optional.ofNullable(responseEntity).map(ResponseEntity::getBody).orElse(null);
    }

    private static <T> boolean anyMatch(T[] array, Predicate<T> predicate) {
        return Stream.of(array).anyMatch(predicate);
    }
}
