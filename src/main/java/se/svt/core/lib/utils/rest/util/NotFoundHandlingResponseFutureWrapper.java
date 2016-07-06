package se.svt.core.lib.utils.rest.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NotFoundHandlingResponseFutureWrapper<T extends Optional<?>> extends ResponseFutureWrapper<T> {

    public NotFoundHandlingResponseFutureWrapper(ListenableFuture<ResponseEntity<T>> delegate) {
        super(delegate);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException executionException) {
            return handleExecutionException(executionException);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (ExecutionException executionException) {
            return handleExecutionException(executionException);
        }
    }

    private T handleExecutionException(ExecutionException executionException) throws ExecutionException {
        if (isNotFoundException(executionException.getCause())) {
            return (T) Optional.empty();
        }
        throw executionException;
    }

    @Override
    NotFoundTranslatingCallback<T> createCallback(SuccessCallback<? super T> successCallback, FailureCallback
        failureCallback) {
        return new NotFoundTranslatingCallback<T>(successCallback, failureCallback);
    }

    private static boolean isNotFoundException(Throwable throwable) {
        return throwable instanceof HttpClientErrorException &&
            ((HttpClientErrorException) throwable).getStatusCode() == HttpStatus.NOT_FOUND;
    }

    static class NotFoundTranslatingCallback<S> extends MyCallback<S> {

        public NotFoundTranslatingCallback(SuccessCallback successCallback, FailureCallback failureCallback) {
            super(successCallback, failureCallback);
        }

        @Override
        public void onFailure(Throwable throwable) {
            if (isNotFoundException(throwable)) {
                successCallback.onSuccess((S) Optional.empty());
            } else {
                super.onFailure(throwable);
            }
        }
    }
}
