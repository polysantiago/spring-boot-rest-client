package io.github.polysantiago.spring.rest;

import io.github.polysantiago.spring.rest.RestClientCompletableFutureAsyncTest.CompletableFutureAsyncFooClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RestClientCompletableFutureAsyncTest extends AbstractRestClientAsyncTest<CompletableFutureAsyncFooClient> {

    @Configuration
    @EnableRestClients(basePackageClasses = CompletableFutureAsyncFooClient.class)
    protected static class TestConfiguration extends BaseTestConfiguration {

    }

    @RestClient(value = "completable-future", url = "${localhost.uri}")
    interface CompletableFutureAsyncFooClient extends AbstractRestClientAsyncTest.AsyncFooClient {

        @Override
        @RequestMapping
        CompletableFuture<String> defaultFoo();

        @Override
        @RequestMapping(value = "/{id}")
        CompletableFuture<String> foo(@PathVariable("id") String id, @RequestParam("query") String query);

        @Override
        @RequestMapping(value = "/foo/{id}")
        CompletableFuture<Foo> getFoo(@PathVariable("id") String id);

        @Override
        @RequestMapping(value = "/fooList", method = RequestMethod.GET)
        CompletableFuture<List<Foo>> fooList();

        @Override
        @RequestMapping(value = "/fooArray", method = RequestMethod.GET)
        CompletableFuture<Foo[]> fooArray();

        @Override
        @RequestMapping(value = "/fooObject", method = RequestMethod.GET)
        CompletableFuture<Object> fooObject();

        @Override
        @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        CompletableFuture<Foo> getFooWithAcceptHeader(@PathVariable("id") String id);

        @Override
        @RequestMapping(value = "/", method = RequestMethod.POST)
        CompletableFuture<Void> bar(String body);

        @Override
        @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
        CompletableFuture<Void> barWithContentType(Foo foo);

        @Override
        @RequestMapping(method = RequestMethod.PUT)
        CompletableFuture<Void> barPut(String body);

        @Override
        @RequestMapping(method = RequestMethod.PATCH)
        CompletableFuture<Void> barPatch(String body);

        @Override
        @RequestMapping(method = RequestMethod.DELETE)
        CompletableFuture<Void> barDelete(String body);

        @Override
        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        CompletableFuture<Optional<Foo>> tryNonEmptyOptional();

        @Override
        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        CompletableFuture<Optional<String>> tryEmptyOptional();

        @Override
        @RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        CompletableFuture<byte[]> raw();

        @Override
        @RequestMapping(headers = "Some-Header:some-value")
        CompletableFuture<Void> fooWithHeaders(@RequestHeader("User-Id") String userId,
                                               @RequestHeader("Password") String password);

        @Override
        @RequestMapping
        CompletableFuture<ResponseEntity<String>> getEntity();

        @Override
        @RequestMapping
        CompletableFuture<HttpEntity<String>> getHttpEntity();

        @Override
        @PostForLocation(value = "/postForLocation")
        CompletableFuture<URI> postForLocation(String body);

    }

}
