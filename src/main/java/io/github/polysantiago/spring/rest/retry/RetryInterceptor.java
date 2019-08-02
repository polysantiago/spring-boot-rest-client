package io.github.polysantiago.spring.rest.retry;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

public class RetryInterceptor {

  private final RetrySettings retrySettings;

  public RetryInterceptor(RetrySettings retrySettings) {
    this.retrySettings = defaultIfNull(retrySettings, new RetrySettings());
  }

  public RetryOperationsInterceptor buildInterceptor() {
    return RetryInterceptorBuilder.stateless()
        .retryPolicy(
            new SimpleRetryPolicy(
                retrySettings.getMaxAttempts(), singletonMap(RetryableException.class, true)))
        .backOffPolicy(getBackOffPolicy())
        .build();
  }

  private BackOffPolicy getBackOffPolicy() {
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
