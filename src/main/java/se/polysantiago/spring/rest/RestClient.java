package se.polysantiago.spring.rest;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestClient {

    /**
     * The serviceId with optional protocol prefix. Either serviceId or url must be specified but not both.
     * Can be an SpEL expression, eg: ${propertyKey}.
     */
    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

    /**
     * An absolute URL or resolvable hostname (the protocol is optional and defaults to HTTP).
     * SpEL expressions are evaluated.
     */
    String url() default "";

    /**
     * The array of {@code HttpStatus} to retry on
     * @see org.springframework.http.HttpStatus
     */
    HttpStatus[] retryOn() default {HttpStatus.SERVICE_UNAVAILABLE};

    /**
     * The exception to retry on
     */
    Class<? extends Exception>[] retryOnException() default {IOException.class};

}
