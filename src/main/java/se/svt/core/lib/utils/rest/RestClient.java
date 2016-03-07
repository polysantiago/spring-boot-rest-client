package se.svt.core.lib.utils.rest;


import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestClient {

    /**
     * The serviceId with optional protocol prefix. Either serviceId or url must be specified but not both. Can be
     * specified as property key, eg: ${propertyKey}.
     */
    @AliasFor("name") String value() default "";

    @AliasFor("value") String name() default "";

    /**
     * An absolute URL or resolvable hostname (the protocol is optional).
     */
    String url() default "";

    HttpStatus[] retryOn() default {};

    Class<? extends Exception>[] retryOnException() default {};

}
