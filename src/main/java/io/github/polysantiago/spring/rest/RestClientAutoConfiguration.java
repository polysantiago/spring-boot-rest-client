package io.github.polysantiago.spring.rest;

import io.github.polysantiago.spring.rest.retry.RetryOperationsInterceptorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.RetryConfiguration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
@ConditionalOnBean(annotation = {EnableRestClients.class})
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@RequiredArgsConstructor
public class RestClientAutoConfiguration {

    private final List<RestClientSpecification> specifications;

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restClientTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RestTemplate.class)
    public AsyncRestTemplate asyncRestClientTemplate(RestTemplate restTemplate) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return new AsyncRestTemplate(requestFactory, restTemplate);
    }

    @Bean
    public WebMvcConfigurer restClientWebMvcConfigurer(RestClientProperties properties) {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addFormatters(FormatterRegistry registry) {
                DateTimeFormatterRegistrar dateTimeFormatterRegistrar = new DateTimeFormatterRegistrar();
                dateTimeFormatterRegistrar.setUseIsoFormat(properties.getIsoDateTimeFormat());
                dateTimeFormatterRegistrar.registerFormatters(registry);
            }
        };
    }

    @Configuration
    @ConditionalOnBean(RetryConfiguration.class)
    protected static class RestClientRetryConfiguration {

        @Bean("restClientRetryInterceptor")
        @ConditionalOnMissingBean
        public RetryOperationsInterceptorFactory retryOperationInterceptorFactory(RestClientProperties properties) {
            return new RetryOperationsInterceptorFactory(properties);
        }

        @Bean
        @ConditionalOnMissingBean
        public RestClientRetryConfigurer restClientRetryConfigurer(RetryOperationsInterceptor restClientRetryInterceptor) {
            return new RestClientRetryConfigurer(restClientRetryInterceptor);
        }

    }

    @Bean
    public RestClientContext restClientContext(RestClientProperties properties) {
        return new RestClientContext(specifications, properties.getServices());
    }

}
