package io.github.polysantiago.spring.rest.retry;

import lombok.Data;

@Data
public class RetrySettings {

    private int maxAttempts = 3;
    private BackOffSettings backOff = new BackOffSettings();

}
