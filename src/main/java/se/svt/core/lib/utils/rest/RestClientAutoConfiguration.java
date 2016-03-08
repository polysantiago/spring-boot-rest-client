package se.svt.core.lib.utils.rest;

import se.svt.core.lib.utils.rest.retry.RetryInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.RetryConfiguration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class RestClientAutoConfiguration {

    @Autowired(required = false)
    private List<RestClientSpecification> specifications;

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();

        //find and replace Jackson message converter with our own
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters().stream()
            .filter(converter -> !(converter instanceof MappingJackson2HttpMessageConverter))
            .collect(toList());

        messageConverters.add(mappingJackson2HttpMessageConverter());

        return new RestTemplate(messageConverters);
    }

    @Bean
    @ConditionalOnMissingBean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        return objectMapper;
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
