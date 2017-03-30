package se.polysantiago.spring.rest;

import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestClient {

    /**
     * The serviceId with optional protocol prefix. Either serviceId or url must be specified but not both. Can be
     * specified as property key, eg: ${propertyKey}.
     */
    String value() default "";

    String name() default "";

    /**
     * An absolute URL or resolvable hostname (the protocol is optional).
     */
    String url() default "";

    HttpStatus[] retryOn() default {HttpStatus.SERVICE_UNAVAILABLE};

    Class<? extends Exception>[] retryOnException() default {IOException.class};

}
