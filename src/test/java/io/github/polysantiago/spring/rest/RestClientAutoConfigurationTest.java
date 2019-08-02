package io.github.polysantiago.spring.rest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.bind.annotation.GetMapping;

public class RestClientAutoConfigurationTest {

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
        assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(MultipleCandidatesConfiguration.class).getBean(TestClient.class))
            .withCauseExactlyInstanceOf(NoUniqueBeanDefinitionException.class);
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
