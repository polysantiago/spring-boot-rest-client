package io.github.polysantiago.spring.rest.retry;

public class RetryableException extends RuntimeException {

    public RetryableException(Throwable cause) {
        super(cause);
    }
}
