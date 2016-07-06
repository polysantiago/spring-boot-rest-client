package se.svt.core.lib.utils.rest.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
public class ResponseFutureWrapper<T> implements ListenableFuture<T> {

    private final ListenableFuture<ResponseEntity<T>> delegate;

    @Override
    public void addCallback(ListenableFutureCallback<? super T> listenableFutureCallback) {
        MyCallback callback = createCallback(listenableFutureCallback, listenableFutureCallback);
        delegate.addCallback(callback);
    }

    @Override
    public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
        delegate.addCallback(createCallback(successCallback, failureCallback));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get().getBody();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit).getBody();
    }

    MyCallback<T> createCallback(SuccessCallback<? super T> successCallback, FailureCallback
        failureCallback) {
        return new MyCallback<T>(successCallback, failureCallback);
    }

    @RequiredArgsConstructor
    static class MyCallback<S> implements ListenableFutureCallback<ResponseEntity<S>> {

        final SuccessCallback<? super S> successCallback;
        final FailureCallback failureCallback;

        @Override
        public void onFailure(Throwable throwable) {
            failureCallback.onFailure(throwable);
        }

        @Override
        public void onSuccess(ResponseEntity<S> tResponseEntity) {
            successCallback.onSuccess(tResponseEntity.getBody());
        }
    }
}
