package se.svt.core.lib.utils.rest.retry;

public class RetryableException extends RuntimeException {

    public RetryableException(Throwable cause) {
        super(cause);
    }
}
