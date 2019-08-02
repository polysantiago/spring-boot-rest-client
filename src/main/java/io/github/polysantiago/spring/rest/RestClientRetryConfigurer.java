package io.github.polysantiago.spring.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@RequiredArgsConstructor
class RestClientRetryConfigurer {

  private final RetryOperationsInterceptor retryOperationsInterceptor;

  void configure(ProxyFactory proxyFactory, RestClientInterceptor restClientInterceptor) {
    proxyFactory.addAdvice(retryOperationsInterceptor);
    restClientInterceptor.setRetryEnabled(true);
  }
}
