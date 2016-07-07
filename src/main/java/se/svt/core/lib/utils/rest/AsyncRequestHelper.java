package se.svt.core.lib.utils.rest;


import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;
import se.svt.core.lib.utils.rest.util.OptionalTypeFutureAdapter;
import se.svt.core.lib.utils.rest.util.ResponseFutureAdapter;
import se.svt.core.lib.utils.rest.util.TypeUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import java.lang.reflect.Method;
import java.util.Optional;

@RequiredArgsConstructor
class AsyncRequestHelper {

    private final AsyncRestTemplate asyncRestTemplate;

    Object executeAsyncRequest(Method method, RequestEntity<Object> requestEntity) {
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
}
