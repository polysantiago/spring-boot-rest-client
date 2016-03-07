package se.svt.core.lib.utils.rest.retry;

import lombok.Data;

@Data
public class RetrySettings {

    private int maxAttempts = 3;
    private BackOffSettings backOff;

}
