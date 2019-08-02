package io.github.polysantiago.spring.rest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.bind.annotation.GetMapping;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class RestClientAutoConfigurationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testInjectConversionServiceSingleCandidate() {
        new AnnotationConfigApplicationContext(SingleCandidateConfiguration.class).getBean(TestClient.class);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients(clients = TestClient.class)
    static class SingleCandidateConfiguration {

        @Bean
        FormattingConversionService someConversionService() {
            return new DefaultFormattingConversionService();
        }

    }

    @Test
    public void testInjectConversionMultipleCandidates() {
        thrown.expectCause(instanceOf(NoUniqueBeanDefinitionException.class));
        new AnnotationConfigApplicationContext(MultipleCandidatesConfiguration.class).getBean(TestClient.class);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients(clients = TestClient.class)
    static class MultipleCandidatesConfiguration {

        @Bean
        FormattingConversionService someConversionService() {
            return new DefaultFormattingConversionService();
        }

        @Bean
        FormattingConversionService someOtherConversionService() {
            return new DefaultFormattingConversionService();
        }

    }

    @Test
    public void testInjectMvcConversionServiceAsDefault() {
        new AnnotationConfigApplicationContext(InjectMvcConversionServiceAsDefaultConfiguration.class).getBean(TestClient.class);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients(clients = TestClient.class)
    static class InjectMvcConversionServiceAsDefaultConfiguration {

        @Bean
        FormattingConversionService someConversionService() {
            return new DefaultFormattingConversionService();
        }

        @Bean
        FormattingConversionService mvcConversionService() {
            return new DefaultFormattingConversionService();
        }

    }

    @RestClient(value = "test-client", url = "http://someserver")
    interface TestClient {

        @GetMapping("str")
        String getString();

    }

}
