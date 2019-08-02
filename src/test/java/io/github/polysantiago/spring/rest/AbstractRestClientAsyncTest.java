package io.github.polysantiago.spring.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.polysantiago.spring.rest.AbstractRestClientAsyncTest.AsyncFooClient;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.toEncodedString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class AbstractRestClientAsyncTest<T extends AsyncFooClient> {

    @Autowired
    T fooClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    MockRestServiceServer asyncServer;

    @TestConfiguration
    @EnableAutoConfiguration
    static class BaseTestConfiguration {

        @Bean
        public AsyncRestTemplate asyncRestTemplate(RestTemplateBuilder builder) {
            return new AsyncRestTemplate(new SimpleClientHttpRequestFactory(), builder.build());
        }

    }

    @Before
    public void setUp() throws Exception {
        asyncServer = createServer(asyncRestTemplate);
    }

    @After
    public void tearDown() throws Exception {
        asyncServer.verify();
    }

    @Test
    public void testRestClientDefaultMapping() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("success", MediaType.TEXT_PLAIN));

        String response = getResponse(fooClient.defaultFoo());

        assertThat(response).isEqualTo("success");
    }

    @Test
    public void testRestClientGet() throws Exception {
        asyncServer.expect(requestTo("http://localhost/some-id?query=some-query"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        fooClient.foo("some-id", "some-query");
    }

    @Test
    public void testRestClientGetObject() throws Exception {
        Foo foo = new Foo("bar");

        asyncServer.expect(requestTo("http://localhost/foo/some-id"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foo), MediaType.APPLICATION_JSON));

        Foo response = getResponse(fooClient.getFoo("some-id"));

        assertThat(response).isNotNull();
        assertThat(response.getBar()).isEqualTo("bar");
    }

    @Test
    public void testRestClientGetList() throws Exception {
        List<Foo> foos = new ArrayList<>(2);
        foos.add(new Foo("bar0"));
        foos.add(new Foo("bar1"));

        asyncServer.expect(requestTo("http://localhost/fooList"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foos), MediaType.APPLICATION_JSON));

        List<Foo> fooList = getResponse(fooClient.fooList());

        Assertions.assertThat(fooList).hasSize(2);
        assertThat(fooList.get(0).getBar()).isEqualTo("bar0");
        assertThat(fooList.get(1).getBar()).isEqualTo("bar1");
    }

    @Test
    public void testRestClientGetArray() throws Exception {
        Foo[] foos = new Foo[]{new Foo("bar0"), new Foo("bar1")};

        asyncServer.expect(requestTo("http://localhost/fooArray"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foos), MediaType.APPLICATION_JSON));

        Foo[] fooArray = getResponse(fooClient.fooArray());

        Assertions.assertThat(fooArray).hasSize(2);
        assertThat(fooArray[0].getBar()).isEqualTo("bar0");
        assertThat(fooArray[1].getBar()).isEqualTo("bar1");
    }

    @Test
    public void testRestClientGetObjectWithNoContentShouldReturnNull() throws Exception {
        asyncServer.expect(requestTo("http://localhost/fooObject"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        try {
            getResponse(fooClient.fooObject());
            fail("Should get NOT FOUND");
        } catch (ExecutionException executionException) {
            assertThat(executionException)
                .hasCauseExactlyInstanceOf(HttpClientErrorException.NotFound.class);
        }
    }

    @Test
    public void testRestClientGetAcceptHeader() throws Exception {
        asyncServer.expect(requestTo("http://localhost/some-id"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
            .andRespond(withSuccess());

        getResponse(fooClient.getFooWithAcceptHeader("some-id"));
    }

    @Test
    public void testRestClientPost() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withCreatedEntity(URI.create("http://some-url")));

        getResponse(fooClient.bar("some-body"));
    }

    @Test
    public void testRestClientPostForLocation() throws Exception {
        URI location = URI.create("http://some-url");
        asyncServer.expect(requestTo("http://localhost/postForLocation"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withCreatedEntity(location));

        Assertions.assertThat(getResponse(fooClient.postForLocation("some-body"))).isEqualTo(location);
    }

    @Test
    public void testRestClientPostAsyncServerError() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        try {
            getResponse(fooClient.bar("some-body"));
            fail("Should have gotten Exception");
        } catch (ExecutionException executionException) {
            assertThat(executionException)
                .hasCauseExactlyInstanceOf(HttpServerErrorException.InternalServerError.class);
        }
    }

    @Test
    public void testRestClientPostWithContentType() throws Exception {
        Foo foo = new Foo("bar");

        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withCreatedEntity(URI.create("http://some-url")));

        getResponse(fooClient.barWithContentType(foo));
    }

    @Test
    public void testRestClientPut() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withSuccess());

        getResponse(fooClient.barPut("some-body"));
    }

    @Test
    public void testRestClientPatch() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withSuccess());

        getResponse(fooClient.barPatch("some-body"));
    }

    @Test
    public void testRestClientDelete() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withSuccess());

        getResponse(fooClient.barDelete("some-body"));
    }

    @Test
    public void testRestClientDoesNotRetryIfNotEnabled() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        try {
            getResponse(fooClient.defaultFoo());
            fail("Should have gotten exception");
        } catch (ExecutionException executionException) {
            assertThat(executionException)
                .hasCauseExactlyInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
        }
    }

    @Test
    public void testRestClientWithNonEmptyOptional() throws Exception {
        Foo foo = new Foo("bar");

        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foo), MediaType.APPLICATION_JSON));

        Assertions.assertThat(getResponse(fooClient.tryNonEmptyOptional())).hasValue(foo);
    }

    @Test
    public void testRestClientWithEmptyOptional() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThat(getResponse(fooClient.tryEmptyOptional())).isNotPresent();
    }

    @Test
    public void testRestClientWithRawData() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("success".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

        byte[] raw = getResponse(fooClient.raw());

        assertThat(toEncodedString(raw, Charset.defaultCharset())).isEqualTo("success");
    }

    @Test
    public void testRestClientWithHeaders() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Some-Header", "some-value"))
            .andExpect(header("User-Id", "userId"))
            .andExpect(header("Password", "password"))
            .andRespond(withSuccess());

        getResponse(fooClient.fooWithHeaders("userId", "password"));
    }

    @Test
    public void testGetEntity() throws Exception {
        String responseString = "RESPSONSE!^$";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("someHeader", "someHeaderValue");
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect((method(HttpMethod.GET)))
            .andRespond(withSuccess(responseString, MediaType.TEXT_PLAIN).headers(responseHeaders));

        ResponseEntity<String> responseEntity = getResponse(fooClient.getEntity());
        assertThat(responseEntity.getBody()).isEqualTo(responseString);
        assertThat(responseEntity.getHeaders().get("someHeader")).isEqualTo(singletonList("someHeaderValue"));
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testGetHttpEntity() throws Exception {
        String responseString = "RESPSONSE!^$";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("someHeader", "someHeaderValue");
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect((method(HttpMethod.GET)))
            .andRespond(withSuccess(responseString, MediaType.TEXT_PLAIN).headers(responseHeaders));

        HttpEntity<String> responseEntity = getResponse(fooClient.getHttpEntity());
        assertThat(responseEntity.getBody()).isEqualTo(responseString);
        assertThat(responseEntity.getHeaders().get("someHeader")).isEqualTo(singletonList("someHeaderValue"));
    }

    interface AsyncFooClient {

        Future<String> defaultFoo();

        Future<String> foo(@PathVariable("id") String id, @RequestParam("query") String query);

        Future<Foo> getFoo(@PathVariable("id") String id);

        Future<List<Foo>> fooList();

        Future<Foo[]> fooArray();

        Future<Object> fooObject();

        Future<Foo> getFooWithAcceptHeader(@PathVariable("id") String id);

        Future<Void> bar(String body);

        Future<Void> barWithContentType(Foo foo);

        Future<Void> barPut(String body);

        Future<Void> barPatch(String body);

        Future<Void> barDelete(String body);

        Future<Optional<Foo>> tryNonEmptyOptional();

        Future<Optional<String>> tryEmptyOptional();

        Future<byte[]> raw();

        Future<Void> fooWithHeaders(@RequestHeader("User-Id") String userId,
                                    @RequestHeader("Password") String password);

        Future<ResponseEntity<String>> getEntity();

        Future<HttpEntity<String>> getHttpEntity();

        Future<URI> postForLocation(String body);

    }

    private <U> U getResponse(Future<U> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

}
