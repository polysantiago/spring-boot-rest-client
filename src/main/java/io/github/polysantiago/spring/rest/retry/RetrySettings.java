package io.github.polysantiago.spring.rest.retry;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetrySettings {

    private int maxAttempts = 3;
    private BackOffSettings backOff = new BackOffSettings();

}
