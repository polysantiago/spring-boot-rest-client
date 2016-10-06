package se.svt.core.lib.utils.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestClientTest {

    @Autowired
    private FooClient fooClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer server;

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients(basePackageClasses = FooClient.class)
    protected static class ContextConfiguration {

    }

    @RestClient(value = "localhost", url = "${localhost.uri}")
    interface FooClient {

        @RequestMapping
        String defaultFoo();

        @RequestMapping(value = "/{id}")
        String foo(@PathVariable("id") String id, @RequestParam("query") String query);

        @RequestMapping(value = "/foo/{id}")
        Foo getFoo(@PathVariable("id") String id);

        @RequestMapping(value = "/fooList", method = RequestMethod.GET)
        List<Foo> fooList();

        @RequestMapping(value = "/fooListParams", method = RequestMethod.GET)
        Void fooListArgument(@RequestParam("myList") List<String> params);

        @RequestMapping(value = "/fooArray", method = RequestMethod.GET)
        Foo[] fooArray();

        @RequestMapping(value = "/fooObject", method = RequestMethod.GET)
        Object fooObject();

        @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        Foo getFooWithAcceptHeader(@PathVariable("id") String id);

        @RequestMapping(value = "/", method = RequestMethod.POST)
        Void bar(String body);

        @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
        Void barWithContentType(Foo foo);

        @RequestMapping(value = "/")
        Void batWithObjectAsParameters(
            @RequestHeader("header") Foo foo1,
            @RequestParam("date") LocalDate date,
            @RequestParam("obj") Foo foo2);

        @RequestMapping(method = RequestMethod.PUT)
        Void barPut(String body);

        @RequestMapping(method = RequestMethod.PATCH)
        Void barPatch(String body);

        @RequestMapping(method = RequestMethod.DELETE)
        Void barDelete(String body);

        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        Optional<Foo> tryNonEmptyOptional();

        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        Optional<String> tryEmptyOptional();

        @RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        byte[] raw();

        @RequestMapping(headers = "Some-Header:some-value")
        Void fooWithHeaders(@RequestHeader("User-Id") String userId, @RequestHeader("Password") String password);

        @RequestMapping
        ResponseEntity<String> getEntity();

        @RequestMapping
        HttpEntity<String> getHttpEntity();


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
    public void testRestClientDefaultMapping() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("success", MediaType.TEXT_PLAIN));

        String response = fooClient.defaultFoo();

        assertThat(response).isEqualTo("success");
    }

    @Test
    public void testRestClientGet() throws Exception {
        server.expect(requestTo("http://localhost/some-id?query=some-query"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        fooClient.foo("some-id", "some-query");
    }

    @Test
    public void testRestClientGetObject() throws Exception {
        Foo foo = new Foo("bar");

        server.expect(requestTo("http://localhost/foo/some-id"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foo), MediaType.APPLICATION_JSON));

        Foo response = fooClient.getFoo("some-id");

        assertThat(response).isNotNull();
        assertThat(response.getBar()).isEqualTo("bar");
    }

    @Test
    public void testRestClientGetList() throws Exception {
        List<Foo> foos = newArrayListWithCapacity(2);
        foos.add(new Foo("bar0"));
        foos.add(new Foo("bar1"));

        server.expect(requestTo("http://localhost/fooList"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foos), MediaType.APPLICATION_JSON));

        List<Foo> fooList = fooClient.fooList();

        assertThat(fooList).hasSize(2);
        assertThat(fooList.get(0).getBar()).isEqualTo("bar0");
        assertThat(fooList.get(1).getBar()).isEqualTo("bar1");
    }

    @Test
    public void testRestClientListAsParam() throws Exception {
        List<String> params = newArrayList("abc", "cba");

        server.expect(requestTo("http://localhost/fooListParams?myList=abc&myList=cba"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        fooClient.fooListArgument(params);
    }

    @Test
    public void testRestClientGetArray() throws Exception {
        Foo[] foos = new Foo[]{new Foo("bar0"), new Foo("bar1")};

        server.expect(requestTo("http://localhost/fooArray"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foos), MediaType.APPLICATION_JSON));

        Foo[] fooArray = fooClient.fooArray();

        assertThat(fooArray).hasSize(2);
        assertThat(fooArray[0].getBar()).isEqualTo("bar0");
        assertThat(fooArray[1].getBar()).isEqualTo("bar1");
    }

    @Test
    public void testRestClientGetObjectWithNoContentShouldReturnNull() throws Exception {
        server.expect(requestTo("http://localhost/fooObject"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        try {
            fooClient.fooObject();
            fail("Should get NOT FOUND");
        } catch (HttpClientErrorException exception) {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void testRestClientGetAcceptHeader() throws Exception {
        server.expect(requestTo("http://localhost/some-id"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
            .andRespond(withSuccess());

        fooClient.getFooWithAcceptHeader("some-id");
    }

    @Test
    public void testRestClientPost() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withCreatedEntity(URI.create("http://some-url")));

        fooClient.bar("some-body");
    }

    @Test
    public void testRestClientPostWithContentType() throws Exception {
        Foo foo = new Foo("bar");

        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withCreatedEntity(URI.create("http://some-url")));

        fooClient.barWithContentType(foo);
    }

    @Test
    public void testRestClientSerializeRequestParam() throws Exception {
        LocalDate today = LocalDate.now();
        Foo foo = new Foo("bar");

        String uri = UriComponentsBuilder.fromUriString("http://localhost/")
            .queryParam("date", today.format(DateTimeFormatter.ISO_DATE))
            .queryParam("obj", foo.toString())
            .toUriString();

        server.expect(requestTo(uri))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("header", foo.toString()))
            .andRespond(withSuccess());

        fooClient.batWithObjectAsParameters(foo, today, foo);
    }

    @Test
    public void testRestClientPut() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withSuccess());

        fooClient.barPut("some-body");
    }

    @Test
    public void testRestClientPatch() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withSuccess());

        fooClient.barPatch("some-body");
    }

    @Test
    public void testRestClientDelete() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withSuccess());

        fooClient.barDelete("some-body");
    }

    @Test(expected = HttpServerErrorException.class)
    public void testRestClientDoesNotRetryIfNotEnabled() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        fooClient.defaultFoo();
    }

    @Test
    public void testRestClientWithNonEmptyOptional() throws Exception {
        Foo foo = new Foo("bar");

        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(foo), MediaType.APPLICATION_JSON));

        assertThat(fooClient.tryNonEmptyOptional()).hasValue(foo);
    }

    @Test
    public void testRestClientWithEmptyOptional() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(fooClient.tryEmptyOptional()).isNotPresent();
    }

    @Test
    public void testRestClientWithRawData() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("success".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

        byte[] raw = fooClient.raw();

        assertThat(StringUtils.toEncodedString(raw, Charset.defaultCharset())).isEqualTo("success");
    }

    @Test
    public void testRestClientWithHeaders() throws Exception {
        server.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Some-Header", "some-value"))
            .andExpect(header("User-Id", "userId"))
            .andExpect(header("Password", "password"))
            .andRespond(withSuccess());

        fooClient.fooWithHeaders("userId", "password");
    }

    @Test
    public void testGetEntity() throws Exception {
        String responseString = "RESPSONSE!^$";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("someHeader", "someHeaderValue");
        server.expect(requestTo("http://localhost/"))
            .andExpect((method(HttpMethod.GET)))
            .andRespond(withSuccess(responseString, MediaType.TEXT_PLAIN).headers(responseHeaders));

        ResponseEntity<String> responseEntity = fooClient.getEntity();
        assertThat(responseEntity.getBody()).isEqualTo(responseString);
        assertThat(responseEntity.getHeaders().get("someHeader")).isEqualTo(singletonList("someHeaderValue"));
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testGetHttpEntity() throws Exception {
        String responseString = "RESPSONSE!^$";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("someHeader", "someHeaderValue");
        server.expect(requestTo("http://localhost/"))
            .andExpect((method(HttpMethod.GET)))
            .andRespond(withSuccess(responseString, MediaType.TEXT_PLAIN).headers(responseHeaders));

        HttpEntity<String> responseEntity = fooClient.getHttpEntity();
        assertThat(responseEntity.getBody()).isEqualTo(responseString);
        assertThat(responseEntity.getHeaders().get("someHeader")).isEqualTo(singletonList("someHeaderValue"));
    }
}
