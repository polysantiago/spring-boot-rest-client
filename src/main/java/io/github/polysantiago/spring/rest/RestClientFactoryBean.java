package io.github.polysantiago.spring.rest;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@Setter
class RestClientFactoryBean<T>
    implements FactoryBean<T>, InitializingBean, ApplicationContextAware {

  private static final String PREFERRED_CONVERSION_SERVICE = "mvcConversionService";
  private static final FormattingConversionService DEFAULT_CONVERSION_SERVICE =
      new DefaultFormattingConversionService();

  private String name;

  private String url;

  @Getter private Class<T> objectType;

  private ApplicationContext applicationContext;

  @Override
  public void afterPropertiesSet() {
    Assert.hasText(this.name, "Name must be set");
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getObject() {
    RestTemplate restTemplate = applicationContext.getBean(RestTemplate.class);
    AsyncRestTemplate asyncRestTemplate = applicationContext.getBean(AsyncRestTemplate.class);
    RestClientContext context = applicationContext.getBean(RestClientContext.class);

    RestClientSpecification specification = context.findByRestClientName(name);
    FormattingConversionService conversionService = getConversionService();

    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.addInterface(objectType);

    SyncRequestHelper syncRequestHelper =
        new SyncRequestHelper(specification, restTemplate, objectType);
    AsyncRequestHelper asyncRequestHelper = new AsyncRequestHelper(asyncRestTemplate, objectType);

    RestClientInterceptor interceptor =
        new RestClientInterceptor(
            syncRequestHelper, asyncRequestHelper, conversionService, getServiceUrl(context));

    retryConfigurer().ifPresent(configurer -> configurer.configure(proxyFactory, interceptor));

    proxyFactory.addAdvice(interceptor);

    return (T) proxyFactory.getProxy(applicationContext.getClassLoader());
  }

  private FormattingConversionService getConversionService() {
    Map<String, FormattingConversionService> map =
        applicationContext.getBeansOfType(FormattingConversionService.class);
    if (map.containsKey(PREFERRED_CONVERSION_SERVICE)) {
      return map.get(PREFERRED_CONVERSION_SERVICE);
    } else if (map.size() > 1) {
      throw new NoUniqueBeanDefinitionException(FormattingConversionService.class, map.keySet());
    }
    return map.values().stream().findFirst().orElse(DEFAULT_CONVERSION_SERVICE);
  }

  private URI getServiceUrl(RestClientContext context) {
    if (isEmpty(url)) {
      return context.findServiceUriByName(name);
    }
    return URI.create(url);
  }

  private Optional<RestClientRetryConfigurer> retryConfigurer() {
    try {
      return Optional.of(applicationContext.getBean(RestClientRetryConfigurer.class));
    } catch (NoSuchBeanDefinitionException ex) {
      return Optional.empty();
    }
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
