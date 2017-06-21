package io.github.polysantiago.spring.rest.retry;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackOffSettings {

    private long delay = 1000;
    private long maxDelay;
    private double multiplier;
    private boolean random;

}
