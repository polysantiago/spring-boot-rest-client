package se.svt.core.lib.utils.rest.retry;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.backoff.*;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

@RequiredArgsConstructor
public class RetryInterceptor {

    private final RetrySettings retrySettings;

    public RetryOperationsInterceptor buildInterceptor() {
        return RetryInterceptorBuilder.stateless()
            .retryPolicy(new SimpleRetryPolicy(retrySettings.getMaxAttempts(), ImmutableMap.of(RetryableException.class, true)))
            .backOffPolicy(getBackoffPolicy())
            .build();
    }

    private BackOffPolicy getBackoffPolicy() {
        BackOffSettings backOff = retrySettings.getBackOff();
        long min = backOff.getDelay();
        long max = backOff.getMaxDelay();
        if (backOff.getMultiplier() > 0) {
            ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
            if (backOff.isRandom()) {
                policy = new ExponentialRandomBackOffPolicy();
            }
            policy.setInitialInterval(min);
            policy.setMultiplier(backOff.getMultiplier());
            policy.setMaxInterval(max > min ? max : ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL);
            return policy;
        }
        if (max > min) {
            UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
            policy.setMinBackOffPeriod(min);
            policy.setMaxBackOffPeriod(max);
            return policy;
        }
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(min);
        return policy;
    }

}
