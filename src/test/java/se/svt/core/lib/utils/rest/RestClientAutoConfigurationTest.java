package se.svt.core.lib.utils.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;

@RunWith(SpringRunner.class)
public class RestClientAutoConfigurationTest {

    @Test
    public void testInjectConversionServiceSingleCandidate() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext
            (SingleCandidateConfiguration.class);
        TestClient restClient = applicationContext.getBean(TestClient.class);
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

    @Test(expected = BeanCreationException.class)
    public void testInjectConversionMultipleCandidates() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext
            (MultipleCandidatesConfiguration.class);
        TestClient restClient = applicationContext.getBean(TestClient.class);
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
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext
            (InjectMvcConversionServiceAsDefaultConfiguration.class);
        TestClient restClient = applicationContext.getBean(TestClient.class);
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

    @RestClient(value = "localhost", url = "http://someserver")
    interface TestClient {

        @GetMapping("str")
        String getString();

    }

}
