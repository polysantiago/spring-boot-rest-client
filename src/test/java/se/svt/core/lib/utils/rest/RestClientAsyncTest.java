package se.svt.core.lib.utils.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@SpringApplicationConfiguration(classes = RestClientAsyncTest.TestConfiguration.class)
public class RestClientAsyncTest {

    @Autowired
    private AsyncFooClient fooClient;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer asyncServer;

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients(basePackageClasses = AsyncFooClient.class)
    protected static class TestConfiguration {

        @Bean
        public AsyncRestTemplate asyncRestTemplate() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new Jdk8Module());

            MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
            jacksonConverter.setObjectMapper(objectMapper);

            AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();

            //find and replace Jackson message converter with our own
            List<HttpMessageConverter<?>> messageConverters = asyncRestTemplate.getMessageConverters().stream()
                .filter(converter -> !(converter instanceof MappingJackson2HttpMessageConverter))
                .collect(toList());
            messageConverters.add(jacksonConverter);

            asyncRestTemplate.setMessageConverters(messageConverters);
            return asyncRestTemplate;
        }

    }

    @RestClient(value = "localhost", url = "${localhost.uri}")
    interface AsyncFooClient {

        @RequestMapping
        ListenableFuture<String> defaultFoo();

        @RequestMapping(value = "/{id}")
        ListenableFuture<String> foo(@PathVariable("id") String id, @RequestParam("query") String query);

        @RequestMapping(value = "/foo/{id}")
        ListenableFuture<Foo> getFoo(@PathVariable("id") String id);

        @RequestMapping(value = "/fooList", method = RequestMethod.GET)
        ListenableFuture<List<Foo>> fooList();

        @RequestMapping(value = "/fooArray", method = RequestMethod.GET)
        ListenableFuture<Foo[]> fooArray();

        @RequestMapping(value = "/fooObject", method = RequestMethod.GET)
        ListenableFuture<Object> fooObject();

        @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Foo> getFooWithAcceptHeader(@PathVariable("id") String id);

        @RequestMapping(value = "/", method = RequestMethod.POST)
        ListenableFuture<Void> bar(String body);

        @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Void> barWithContentType(Foo foo);

        @RequestMapping(method = RequestMethod.PUT)
        ListenableFuture<Void> barPut(String body);

        @RequestMapping(method = RequestMethod.PATCH)
        ListenableFuture<Void> barPatch(String body);

        @RequestMapping(method = RequestMethod.DELETE)
        ListenableFuture<Void> barDelete(String body);

        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Optional<Foo>> tryNonEmptyOptional();

        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Optional<String>> tryEmptyOptional();

        @RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        ListenableFuture<byte[]> raw();

        @RequestMapping(headers = "Some-Header:some-value")
        ListenableFuture<Void> fooWithHeaders(@RequestHeader("User-Id") String userId,
                                              @RequestHeader("Password") String password);

        @RequestMapping
        ListenableFuture<ResponseEntity<String>> getEntity();

        @RequestMapping
        ListenableFuture<HttpEntity<String>> getHttpEntity();

    }

    @Before
    public void setUp() throws Exception {
        asyncServer = createServer(asyncRestTemplate);
    }

    @After
    public void tearDown() throws Exception {
        asyncServer.verify();
    }

    private static <T> T getResponse(ListenableFuture<T> listenableFuture) throws Exception {
        return listenableFuture.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void testRestClientDefaultMapping() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("success", MediaType.TEXT_PLAIN));

        String response = getResponse(fooClient.defaultFoo());

        assertThat(response, is("success"));
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

        assertThat(response, is(notNullValue()));
        assertThat(response.getBar(), is("bar"));
    }

    @Test
    public void testRestClientGetList() throws Exception {
        List<Foo> foos = newArrayListWithCapacity(2);
        foos.add(new Foo("bar0"));
        foos.add(new Foo("bar1"));

        asyncServer.expect(requestTo("http://localhost/fooList"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foos), MediaType.APPLICATION_JSON));

        List<Foo> fooList = getResponse(fooClient.fooList());

        assertThat(fooList, hasSize(2));
        assertThat(fooList.get(0).getBar(), is("bar0"));
        assertThat(fooList.get(1).getBar(), is("bar1"));
    }

    @Test
    public void testRestClientGetArray() throws Exception {
        Foo[] foos = new Foo[]{new Foo("bar0"), new Foo("bar1")};

        asyncServer.expect(requestTo("http://localhost/fooArray"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foos), MediaType.APPLICATION_JSON));

        Foo[] fooArray = getResponse(fooClient.fooArray());

        assertThat(fooArray, arrayWithSize(2));
        assertThat(fooArray[0].getBar(), is("bar0"));
        assertThat(fooArray[1].getBar(), is("bar1"));
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
    public void testRestClientPostAsyncServerError() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        try {
            getResponse(fooClient.bar("some-body"));
            fail("Should have gotten Exception");
        } catch (ExecutionException executionException) {
            assertTrue(executionException.getCause() instanceof HttpServerErrorException);
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
            assertTrue(executionException.getCause() instanceof HttpServerErrorException);
        }
    }

    @Test
    public void testRestClientWithNonEmptyOptional() throws Exception {
        Foo foo = new Foo("bar");

        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foo), MediaType.APPLICATION_JSON));

        Optional<Foo> optional = getResponse(fooClient.tryNonEmptyOptional());

        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get(), is(foo));
    }

    @Test
    public void testRestClientWithEmptyOptional() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<String> optional = getResponse(fooClient.tryEmptyOptional());

        assertThat(optional.isPresent(), is(false));
    }

    @Test
    public void testRestClientWithEmptyOptionalAndCallback() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));
        ListenableFutureCallback<Optional<String>> callback = mock(ListenableFutureCallback.class);

        fooClient.tryEmptyOptional().addCallback(callback);

        verify(callback, timeout(10000)).onSuccess(eq(Optional.empty()));
    }

    @Test
    public void testRestClientWithRawData() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("success".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

        byte[] raw = getResponse(fooClient.raw());

        assertThat(StringUtils.toEncodedString(raw, Charset.defaultCharset()), is("success"));
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
        assertThat(responseEntity.getBody(), is(responseString));
        assertThat(responseEntity.getHeaders().get("someHeader"), is(singletonList("someHeaderValue")));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
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
        assertThat(responseEntity.getBody(), is(responseString));
        assertThat(responseEntity.getHeaders().get("someHeader"), is(singletonList("someHeaderValue")));
    }
}
