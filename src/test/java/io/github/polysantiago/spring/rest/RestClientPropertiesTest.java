package io.github.polysantiago.spring.rest;

import io.github.polysantiago.spring.rest.retry.BackOffSettings;
import io.github.polysantiago.spring.rest.retry.RetrySettings;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class RestClientPropertiesTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @After
    public void tearDown() throws Exception {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void testDefaultProperties() throws Exception {
        registerAndRefresh();

        assertProperties(getProperties(), 3, 1000L, 0L, 0.0d, false);
    }

    @Test
    public void testMaxAttempts() throws Exception {
        TestPropertyValues.of("spring.rest.client.retry.max-attempts:5").applyTo(this.context);

        registerAndRefresh();

        assertProperties(getProperties(), 5, 1000L, 0L, 0.0d, false);
    }

    @Test
    public void testBackOffSettings() throws Exception {
        TestPropertyValues.of("spring.rest.client.retry.back-off.delay:2000")
            .and("spring.rest.client.retry.back-off.max-delay:10000")
            .and("spring.rest.client.retry.back-off.multiplier:2.5")
            .and("spring.rest.client.retry.back-off.random:true")
            .applyTo(this.context);

        registerAndRefresh();

        assertProperties(getProperties(), 3, 2000L, 10000L, 2.5d, true);
    }

    private RestClientProperties getProperties() {
        return this.context.getBean(RestClientProperties.class);
    }

    private void registerAndRefresh() {
        this.context.register(PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
        this.context.refresh();
    }

    private static void assertProperties(RestClientProperties properties,
                                         int maxAttempts, long delay, long maxDelay, double multiplier, boolean random) {
        assertThat(properties).isNotNull();

        RetrySettings retry = properties.getRetry();
        assertThat(retry).isNotNull();
        assertThat(retry.getMaxAttempts()).isEqualTo(maxAttempts);

        BackOffSettings backOff = retry.getBackOff();
        assertThat(backOff).isNotNull();
        assertThat(backOff.getDelay()).isEqualTo(delay);
        assertThat(backOff.getMaxDelay()).isEqualTo(maxDelay);
        assertThat(backOff.getMultiplier()).isEqualTo(multiplier);
        assertThat(backOff.isRandom()).isEqualTo(random);
    }

    @Configuration
    @EnableConfigurationProperties(RestClientProperties.class)
    static class TestConfiguration {

    }
}
