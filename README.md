Spring Boot Rest Client
=======

[![Build Status](https://travis-ci.org/polysantiago/spring-boot-rest-client.svg?branch=master)](https://travis-ci.org/polysantiago/spring-boot-rest-client)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.polysantiago/spring-boot-rest-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.polysantiago/spring-boot-rest-client)
[![GitHub issues](https://img.shields.io/github/issues/polysantiago/spring-boot-rest-client.svg)](https://github.com/polysantiago/spring-boot-rest-client/issues)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/polysantiago/spring-boot-rest-client/master/LICENSE)

This library was born as an effort to avoid boilerplate code and making use of Spring Boot's auto-configuration features.

It is based on [Spring Cloud Feign](http://projects.spring.io/spring-cloud/spring-cloud.html#spring-cloud-feign) but it 
uses [RestTemplate](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html) 
instead of Netflix's Feign and [Spring MVC annotations](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestMapping.html).

As an additional feature, **spring-boot-rest-client** supports [Spring Retry](https://github.com/spring-projects/spring-retry) 
so that HTTP requests can be retried upon either specific HTTP statuses and/or defined Exceptions.

Usage
-----

```java
@EnableRestClients
@SpringBootApplication
public class FooApplication {
    
    public static void main(String... args) {
        SpringApplication.run(FooApplication.class, args);
    }
    
    @RestClient("foo")
    interface FooClient {
    
        @RequestMapping
        Foo getFoo();
    
    }
    
}
```

```yaml
spring:
  rest:
    client:
      services:
        foo: http://foo.bar.se
```
    
You can later use `@Autowired` (or constructor injection) and just call `fooClient.getFoo()` which will 
make an `HTTP GET` call to `http://foo.bar.se`

```java
@Component
public class RestClientConsumer {
    
    private final FooClient fooClient;
    
    RestClientConsumer(FooClient fooClient) {
        this.fooClient = fooClient;
    }
    
    public Foo getFoo() {
        return fooClient.getFoo();
    }
    
}
```

Structure
-----
`@RequestMapping` values have the following correspondence to the resulting HTTP call:
    
* `value()` - Path appended to the host
* `method()` - The HTTP method (GET is the default)
* `produces()` - Value of the [Accept](https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1) header
* `consumes()` - Value of the [Content-Type](https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17) header
* `headers()` - `String[]` of key-value pairs of headers separated by ':'

All HTTP REST methods are supported (GET, POST, PUT, PATCH and DELETE) as well as the following annotations on parameters:

* [RequestParam](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html)
* [PathVariable](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/PathVariable.html)
* [RequestHeader](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestHeader.html)
* [RequestBody](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestBody.html)
 
A method parameter with no annotation is expected to be the request body (payload) of the request 
if `@RequestBody` is not specified.

In addition to `@RequestMapping`, composed variants introduced in Spring MVC 4.3 can also be used.
Check [this](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html#mvc-ann-requestmapping-composed)
for more details.

Async
-----
Spring Boot Rest Template can be also be configured to be used for asynchronous REST calls for which it will instead use
an `AsyncRestTemplate` bean. It supports both Oracle's [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) 
as well as Spring's [ListenableFuture](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/concurrent/ListenableFuture.html).

```java
@RestClient("foo")
interface FooClient {
    
    @RequestMapping("/{id}")
    ListenableFuture<String> foo(@PathVariable("id") String id, @RequestParam("query") String query);
    
    @RequestMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    CompletableFuture<Foo> getFoo(@PathVariable("id") String id);
    
}
```

Please note that retry functionality is currently not supported for asynchronous requests.

Generics
-----
Generic declarations are supported as long as the "implementing" interface contains a concrete class.

Working example:

```java
interface FooBaseClient<T> {
    
    @GetMapping(value = "/{id}")
    T getParameterized(@PathVariable("id") String id);
    
}
```
```java
@RestClient("foo")
interface FooClient extends FooBaseClient<Foo> {
    
}
```

HTTP Entities
-----
If for some reason you do not wish to have the body extracted from your response, you can wrap your response type in
either a [ResponseEntity](http://docs.spring.io/autorepo/docs/spring/current/javadoc-api/org/springframework/http/ResponseEntity.html) 
as well as an [HttpEntity](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/HttpEntity.html).

```java
@RestClient("foo")
interface FooClient {
    
    @GetMapping
    ResponseEntity<String> getEntity();
    
    @GetMapping
    HttpEntity<String> getHttpEntity();
    
}
```

JDK 8 Support
-----
If you wrap your response type in Oracle's JDK 8 [Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html),
Spring Boot Rest Client will return an `Optional.empty()` upon a `HTTP 404 NOT FOUND` response code.

```java
@RestClient("foo")
interface FooClient {
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    Optional<Foo> getOptional();
    
}
```

Please note that **default** methods in interfaces declaring `@RestClient` are currently not supported.

HATEOAS Support
-----

Specially useful for [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS), you can use the `@PostForLocation` annotation to
indicate that a POST request should return the `Location` HTTP header as an `URI`.

```java
@RestClient("foo")
interface FooClient {
    
    @PostForLocation("/postForLocation")
    URI postForLocation(String body);
    
}
```

Additionally, by including [Spring HATEOAS](https://github.com/spring-projects/spring-hateoas) as a dependency, you can 
use Spring HATEOAS resource support:

```java
public class FooResource extends ResourceSupport {
    
}
```
```java
@RestClient("foo")
interface FooClient {
  
    @GetMapping(value = "/foo/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    FooResource getFoo(@PathVariable("id") String id);
    
    @GetMapping(value = "/foo/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    Resource<Foo> getFooWrapped(@PathVariable("id") String id);
    
    @GetMapping(value = "/foos", produces = MediaTypes.HAL_JSON_VALUE)
    Resources<FooResource> getFoos();
    
    @GetMapping(value = "/foos", produces = MediaTypes.HAL_JSON_VALUE)
    PagedResources<FooResource> getPagedFoos();
    
}
```
 
Retry
-----

The rest client library can be used with Spring Retry. Just by adding the `org.springframework.retry:spring-retry` 
library as a dependency and `@EnableRetry` in your configuration, the retry functionality will be enabled.
By default, calls are retried on `HTTP 503 SERVICE UNAVAILABLE` and `IOException` but you can configure your own:

```java
@RestClient(
    value = "foo", 
    retryOn = {HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY}, 
    retryOnException = SocketTimeoutException.class)
interface FooClient {
    
    @RestClient("/foos")
    List<Foo> getFooList();
    
}
```
    
Furthermore, global retry settings can be configured by adding values to `application.yml`. Below, the default values
are shown:

```yaml
spring:
  rest:
    client:
      services:
        foo: http://foo.bar.se
      retry:
        max-attempts: 3
        back-off:
          delay: 1000
          max-delay: 0
          multiplier: 0.0
          random: false
```

Refer to [Spring Retry](https://github.com/spring-projects/spring-retry) for more information about what the values refer to.
    
Miscellaneous
-------------

* The library will create a `RestTemplate` and a `AsyncRestTemplate` Spring beans if not already present using a
[RestTemplateBuilder](http://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/web/client/RestTemplateBuilder.html)
* The library is non-intrusive. That means that if you want the spring-retry functionality you'll need to include it and 
all of its dependencies
* `@RestClient` also accepts an optional `url()` parameter which can be either a hardcoded value 
or a [SpEL expression](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/html/expressions.html) 

TODO
----

* Add option to disable retry on either clients or specific methods
* Support `@Recover` method as specified in Spring Retry when retries are exhausted