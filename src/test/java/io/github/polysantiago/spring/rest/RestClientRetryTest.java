package io.github.polysantiago.spring.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestClientRetryTest {

    @Autowired
    private BazClient bazClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${localhost.uri}")
    private String requestUrl;

    private MockRestServiceServer server;

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients(basePackageClasses = BazClient.class)
    @EnableRetry
    protected static class TestConfiguration {

    }

    @RestClient(name = "baz-client", retryOn = {HttpStatus.SERVICE_UNAVAILABLE}, retryOnException = {ResourceAccessException.class})
    interface BazClient {

        @RequestMapping
        Void foo();

    }

    @Before
    public void setUp() throws Exception {
        server = createServer(restTemplate);
    }

    @After
    public void tearDown() throws Exception {
        server.verify();
    }

    @Test
    public void testRetry() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        bazClient.foo();
    }

    @Test(expected = HttpServerErrorException.class)
    public void testShouldNotRetry() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());

        bazClient.foo();
    }

    @Test
    public void testShouldRetryOnIoException() throws Exception {
        // This is a workaround needed because of a glitch in SimpleRequestExpectationManager.
        // If using SimpleRequestExpectationManager, afterExpectationsDeclared() fails due to the request not properly
        // being registered as the response throws an Exception.
        MockRestServiceServer unorderedServer = bindTo(restTemplate).ignoreExpectOrder(true).build();

        unorderedServer.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(request -> {
                throw new IOException();
            });

        unorderedServer.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        bazClient.foo();
    }

    @Test(expected = RuntimeException.class)
    public void testShouldNotRetryOnOtherExceptions() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(request -> {
                throw new RuntimeException();
            });

        bazClient.foo();
    }

    private String defaultUrl() {
        return requestUrl + "/";
    }
}
