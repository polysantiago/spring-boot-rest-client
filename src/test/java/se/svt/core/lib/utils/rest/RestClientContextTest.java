package se.svt.core.lib.utils.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestClientContextTest {

    @Order(Ordered.LOWEST_PRECEDENCE)
    @Configuration
    @EnableAutoConfiguration
    @EnableRetry
    @EnableRestClients
    protected static class TestConfiguration {

        @Bean
        public RetryOperationsInterceptor retryInterceptor() {
            return new RetryOperationsInterceptor();
        }

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }

    }

    @Test
    public void testContextLoads() throws Exception {

    }
}
