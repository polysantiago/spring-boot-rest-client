package se.polysantiago.spring.rest;

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
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Setter
class RestClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private static final String PREFERRED_CONVERSION_SERVICE = "mvcConversionService";
    private static final FormattingConversionService DEFAULT_CONVERSION_SERVICE = new DefaultFormattingConversionService();

    private String name;

    private String url;

    @Getter
    private Class<?> objectType;

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(this.name, "Name must be set");
    }

    @Override
    public Object getObject() throws Exception {
        RestTemplate restTemplate = applicationContext.getBean(RestTemplate.class);
        AsyncRestTemplate asyncRestTemplate = applicationContext.getBean(AsyncRestTemplate.class);
        RestClientContext context = applicationContext.getBean(RestClientContext.class);

        RestClientSpecification specification = context.findByRestClientName(name);
        FormattingConversionService conversionService = getConversionService();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addInterface(objectType);

        SyncRequestHelper syncRequestHelper = new SyncRequestHelper(specification, restTemplate);
        AsyncRequestHelper asyncRequestHelper = new AsyncRequestHelper(asyncRestTemplate);

        RestClientInterceptor interceptor = new RestClientInterceptor(
            syncRequestHelper,
            asyncRequestHelper,
            conversionService,
            getServiceUrl(context));

        retryInterceptor().ifPresent(retryOperationsInterceptor -> {
            proxyFactory.addAdvice(retryOperationsInterceptor);
            interceptor.setRetryEnabled(true);
        });

        proxyFactory.addAdvice(interceptor);

        return proxyFactory.getProxy(applicationContext.getClassLoader());
    }

    private FormattingConversionService getConversionService() {
        Map<String, FormattingConversionService> map = applicationContext.getBeansOfType(FormattingConversionService.class);
        if (map.containsKey(PREFERRED_CONVERSION_SERVICE)) {
            return map.get(PREFERRED_CONVERSION_SERVICE);
        } else if (map.size() > 1) {
            throw new NoUniqueBeanDefinitionException(FormattingConversionService.class, map.keySet());
        }
        return map.entrySet().stream()
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(DEFAULT_CONVERSION_SERVICE);
    }

    private URI getServiceUrl(RestClientContext context) {
        if (isEmpty(url)) {
            return context.findServiceUriByName(name);
        }
        return URI.create(url);
    }

    private Optional<RetryOperationsInterceptor> retryInterceptor() {
        try {
            return Optional.of(applicationContext.getBean("restClientRetryInterceptor", RetryOperationsInterceptor.class));
        } catch (NoSuchBeanDefinitionException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
