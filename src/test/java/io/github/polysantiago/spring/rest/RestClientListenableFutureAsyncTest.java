package io.github.polysantiago.spring.rest;

import io.github.polysantiago.spring.rest.RestClientListenableFutureAsyncTest.ListenableFutureAsyncFooClient;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class RestClientListenableFutureAsyncTest extends AbstractRestClientAsyncTest<ListenableFutureAsyncFooClient> {

    @Configuration
    @EnableRestClients(basePackageClasses = ListenableFutureAsyncFooClient.class)
    protected static class TestConfiguration extends BaseTestConfiguration {

    }

    @Mock
    private ListenableFutureCallback<Optional<String>> callback;

    @RestClient(value = "listenable-future", url = "${localhost.uri}")
    interface ListenableFutureAsyncFooClient extends AbstractRestClientAsyncTest.AsyncFooClient {

        @Override
        @RequestMapping
        ListenableFuture<String> defaultFoo();

        @Override
        @RequestMapping(value = "/{id}")
        ListenableFuture<String> foo(@PathVariable("id") String id, @RequestParam("query") String query);

        @Override
        @RequestMapping(value = "/foo/{id}")
        ListenableFuture<Foo> getFoo(@PathVariable("id") String id);

        @Override
        @RequestMapping(value = "/fooList", method = RequestMethod.GET)
        ListenableFuture<List<Foo>> fooList();

        @Override
        @RequestMapping(value = "/fooArray", method = RequestMethod.GET)
        ListenableFuture<Foo[]> fooArray();

        @Override
        @RequestMapping(value = "/fooObject", method = RequestMethod.GET)
        ListenableFuture<Object> fooObject();

        @Override
        @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Foo> getFooWithAcceptHeader(@PathVariable("id") String id);

        @Override
        @RequestMapping(value = "/", method = RequestMethod.POST)
        ListenableFuture<Void> bar(String body);

        @Override
        @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Void> barWithContentType(Foo foo);

        @Override
        @RequestMapping(method = RequestMethod.PUT)
        ListenableFuture<Void> barPut(String body);

        @Override
        @RequestMapping(method = RequestMethod.PATCH)
        ListenableFuture<Void> barPatch(String body);

        @Override
        @RequestMapping(method = RequestMethod.DELETE)
        ListenableFuture<Void> barDelete(String body);

        @Override
        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Optional<Foo>> tryNonEmptyOptional();

        @Override
        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        ListenableFuture<Optional<String>> tryEmptyOptional();

        @Override
        @RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        ListenableFuture<byte[]> raw();

        @Override
        @RequestMapping(headers = "Some-Header:some-value")
        ListenableFuture<Void> fooWithHeaders(@RequestHeader("User-Id") String userId,
                                              @RequestHeader("Password") String password);

        @Override
        @RequestMapping
        ListenableFuture<ResponseEntity<String>> getEntity();

        @Override
        @RequestMapping
        ListenableFuture<HttpEntity<String>> getHttpEntity();

        @Override
        @PostForLocation(value = "/postForLocation")
        ListenableFuture<URI> postForLocation(String body);

    }

    @Test
    public void testRestClientWithEmptyOptionalAndCallback() throws Exception {
        asyncServer.expect(requestTo("http://localhost/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        fooClient.tryEmptyOptional().addCallback(callback);

        verify(callback, timeout(10000)).onSuccess(eq(Optional.empty()));
    }

}
