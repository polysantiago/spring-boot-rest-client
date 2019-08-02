package io.github.polysantiago.spring.rest.retry;

import io.github.polysantiago.spring.rest.RestClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@RequiredArgsConstructor
public class RetryOperationsInterceptorFactory
    extends AbstractFactoryBean<RetryOperationsInterceptor> {

  private final RestClientProperties restClientProperties;

  @Override
  public Class<?> getObjectType() {
    return RetryOperationsInterceptor.class;
  }

  @Override
  protected RetryOperationsInterceptor createInstance() {
    return new RetryInterceptor(restClientProperties.getRetry()).buildInterceptor();
  }
}
