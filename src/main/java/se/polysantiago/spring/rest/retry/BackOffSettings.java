package se.polysantiago.spring.rest.retry;

import lombok.Data;

@Data
public class BackOffSettings {

    private long delay = 1000;
    private long maxDelay;
    private double multiplier;
    private boolean random;

}
