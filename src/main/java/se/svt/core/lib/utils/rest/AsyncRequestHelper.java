package se.svt.core.lib.utils.rest;


import lombok.RequiredArgsConstructor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;
import se.svt.core.lib.utils.rest.util.LocationFutureAdapter;
import se.svt.core.lib.utils.rest.util.OptionalTypeFutureAdapter;
import se.svt.core.lib.utils.rest.util.ResolvableTypeUtils;
import se.svt.core.lib.utils.rest.util.ResponseFutureAdapter;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;

import static se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference.fromResolvableType;

@RequiredArgsConstructor
class AsyncRequestHelper {

    private final AsyncRestTemplate asyncRestTemplate;

    <T> ListenableFuture<?> executeAsyncRequest(Method method, RequestEntity<T> requestEntity) {
        ResolvableType resolvedType = ResolvableType.forMethodReturnType(method).getGeneric(0);

        SyntheticParametrizedTypeReference<T> wrappedReturnType = fromResolvableType(resolvedType);
        if (ResolvableTypeUtils.typeIsAnyOf(resolvedType, HttpEntity.class, ResponseEntity.class)) {
            SyntheticParametrizedTypeReference<T> responseType = fromResolvableType(resolvedType.getGeneric(0));
            return sendAsyncRequest(requestEntity, responseType);
        }

        ListenableFuture<ResponseEntity<T>> listenableFuture = sendAsyncRequest(requestEntity, wrappedReturnType);

        if (hasPostLocation(method)) {
            checkWrappedReturnTypeIsUri(resolvedType);
            return new LocationFutureAdapter<>(listenableFuture);
        }
        if (ResolvableTypeUtils.typeIs(resolvedType, Optional.class)) {
            return new OptionalTypeFutureAdapter<T>(toOptional(listenableFuture));
        }
        return new ResponseFutureAdapter<>(listenableFuture);
    }

    private boolean hasPostLocation(Method method) {
        return AnnotationUtils.findAnnotation(method, PostForLocation.class) != null;
    }

    private <T> ListenableFuture<ResponseEntity<T>> sendAsyncRequest(RequestEntity<T> requestEntity,
                                                                     SyntheticParametrizedTypeReference<T> responseType) {
        return asyncRestTemplate.exchange(requestEntity.getUrl(), requestEntity.getMethod(), requestEntity, responseType);
    }

    private static void checkWrappedReturnTypeIsUri(ResolvableType resolvableType) {
        if (!ResolvableTypeUtils.typeIs(resolvableType, URI.class)) {
            throw new RuntimeException("Method annotated with @PostForLocation must return URI");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ListenableFuture<ResponseEntity<Optional<T>>> toOptional(ListenableFuture<? extends ResponseEntity<?>> future) {
        return (ListenableFuture<ResponseEntity<Optional<T>>>) future;
    }

}
