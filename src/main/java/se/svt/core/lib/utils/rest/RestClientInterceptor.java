package se.svt.core.lib.utils.rest;

import se.svt.core.lib.utils.rest.util.MethodUtils;

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
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
    private final URI serviceUrl;

    public void setRetryEnabled(boolean retryEnabled) {
        syncRequestHelper.setRetryEnabled(true);
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        RequestEntity<Object> requestEntity = RestClientInterceptorHelper.from(methodInvocation).buildRequest(serviceUrl);

        if (MethodUtils.returnTypeIs(method, ListenableFuture.class)) {
            return asyncRequestHelper.executeAsyncRequest(method, requestEntity);
        }
        if (MethodUtils.returnTypeIs(method, CompletableFuture.class)) {
            return toCompletableFuture(asyncRequestHelper.executeAsyncRequest(method, requestEntity));
        }
        return syncRequestHelper.executeRequest(method, requestEntity);
    }

}
