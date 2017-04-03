package io.github.polysantiago.spring.rest;

import io.github.polysantiago.spring.rest.util.ResolvableTypeUtils;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.RequestEntity;
import org.springframework.util.concurrent.ListenableFuture;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static net.javacrumbs.futureconverter.springjava.FutureConverter.toCompletableFuture;

@RequiredArgsConstructor
class RestClientInterceptor implements MethodInterceptor {

    private final SyncRequestHelper syncRequestHelper;
    private final AsyncRequestHelper asyncRequestHelper;
    private final FormattingConversionService conversionService;
    private final URI serviceUrl;

    void setRetryEnabled(boolean retryEnabled) {
        syncRequestHelper.setRetryEnabled(true);
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        RequestEntity<Object> requestEntity = RestClientInterceptorHelper
            .from(methodInvocation)
            .conversionService(conversionService)
            .buildRequest(serviceUrl);

        if (ResolvableTypeUtils.returnTypeIs(method, ListenableFuture.class)) {
            return executeAsync(method, requestEntity);
        }
        if (ResolvableTypeUtils.returnTypeIs(method, CompletableFuture.class)) {
            return toCompletableFuture(executeAsync(method, requestEntity));
        }
        return syncRequestHelper.executeRequest(methodInvocation, requestEntity);
    }

    private ListenableFuture<?> executeAsync(Method method, RequestEntity<Object> requestEntity) {
        return asyncRequestHelper.executeAsyncRequest(method, requestEntity);
    }

}
