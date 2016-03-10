SVT - Declarative Rest Client
=======

This library was born as an effort to avoid boilerplate code and making use of Spring Boot's configuration features.

It is based on [Spring Cloud Feign](http://projects.spring.io/spring-cloud/spring-cloud.html#spring-cloud-feign) but it uses
[RestTemplate](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html) instead of
Netflix's Feign and [Spring MVC annotations](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestMapping.html).

As an additional feature, **core-rest-client** supports [Spring Retry](https://github.com/spring-projects/spring-retry) so that 
HTTP requests can be retried upon either specific HTTP statuses and/or defined Exceptions.

Usage
-----

`FooApplication.class`
    :::java
    @EnableRestClients
    @SpringBootApplication
    public class FooApplication {
    
        public static void main(String... args) {
            SpringApplication.run(FooApplication.class, args);
        }
        
        @RestClient("foo)
        interface FooClient {
        
            @RequestMapping
            Foo getFoo();
        
        }
    
    }

`application.yml`
    ```
    svt:
        rest-client:
            services:
                foo: http://foo.svt.se
    ```
    
You can later use `@Autowired` and just call `fooClient.getFoo()` which will make an `HTTP GET` call to http://foo.svt.se

`@RequestMapping` values have the following correspondence to the resulting HTTP call:
    
    * `value()` - Path appended to the host
    * `method()` - The HTTP method (GET is the default)
    * `produces()` - Value of the [Accept](https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1) header
    * `consumes()` - Value of the [Content-Type](https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17) - `application/octet-stream` is the default
    * `headers()` - `String[]` of key-value pairs of headers separated by ':'

All HTTP REST methods are supported (GET, POST, PUT, PATCH and DELETE) as well as the following annotations on parameters:

    * [RequestParam](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html)
    * [PathVariable](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/PathVariable.html)
    * [RequestHeader](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestHeader.html)    
 
 A method parameter with no annotation is expected to be the request body (payload) of the request.
 
Retry
-----

The rest client library has retry for Spring Retry. Just by adding the spring-retry library as a dependency and `@EnableRetry`
in your configuration, the retry functionality will be enabled. By default, calls are retried on `HTTP 503 SERVICE UNAVAILABLE` and
`IOException` but you can configure your own:

    :::java    
    @RestClient(value = "foo", retryOn = {HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY}, retryOnException = SocketTimeoutException.class}
    interface FooClient {
        
        @RestClient(value = "/foos")
        List<Foo> getFooList();
        
    }
    
Furthermore, global retry settings can be configured by adding values to `application.yml`. Below, the default values
are shown:

    ```
    svt:
        rest-client:
            services:
                foo: http://foo.svt.se
            retry:
                max-attempts: 0
                back-off:
                    delay: 1000
                    max-delay: 0
                    multiplier: 0.0
                    random: false
    ```
Refer to [Spring Retry](https://github.com/spring-projects/spring-retry) for more information about what the values refer to.
    
Miscellaneous
-------------

    * If the return type of the method is `Optional<T>` and the request returns an HTTP 404 NOT FOUND, then the result is `Optional.empty()`
    * The library will create a `RestTemplate` Spring bean if not already present
    * The library is non-intrusive. That means that if you want the spring-retry functionality you'll need to include it and all it dependencies yourself
    * `@RestClient` also accepts an optional `url()` parameter which can be either a hardcoded value or a 
    [SpEL expression](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/html/expressions.html)
    

TODO
----

    * Add support for AsyncRestTemplate in order to make asynchronous HTTP calls
    * Add option to disable retry on either clients or specific methods
    * Support `@Recover` method as specified in Spring Retry when retries are exhausted

