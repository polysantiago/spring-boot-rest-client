package se.svt.core.lib.utils.rest;

import se.svt.core.lib.utils.rest.retry.RetryInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.RetryConfiguration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class RestClientAutoConfiguration {

    @Autowired(required = false)
    private List<RestClientSpecification> specifications;

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Configuration
    @ConditionalOnBean(RetryConfiguration.class)
    protected static class RestClientRetryConfiguration {

        @Autowired
        private RestClientProperties properties;

        @Bean
        @ConditionalOnMissingBean
        public RetryInterceptor retryInterceptor() {
            return new RetryInterceptor(properties.getRetry());
        }

        @Bean
        @ConditionalOnMissingBean
        public RetryOperationsInterceptor retryOperationsInterceptor() {
            return retryInterceptor().buildInterceptor();
        }

    }

    @Bean
    public RestClientContext restClientContext(RestClientProperties properties) {
        return new RestClientContext(specifications, properties.getServices());
    }

}
