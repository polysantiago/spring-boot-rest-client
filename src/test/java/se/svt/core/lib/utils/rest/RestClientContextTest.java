package se.svt.core.lib.utils.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@SpringApplicationConfiguration(classes = RestClientContextTest.TestConfiguration.class)
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
