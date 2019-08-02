package io.github.polysantiago.spring.rest;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestClient {

  /**
   * The serviceId with optional protocol prefix. Either serviceId or url must be specified but not
   * both. Can be an SpEL expression, eg: ${propertyKey}.
   */
  @AliasFor("name")
  String value() default "";

  @AliasFor("value")
  String name() default "";

  /**
   * An absolute URL or resolvable hostname (the protocol is optional and defaults to HTTP). SpEL
   * expressions are evaluated.
   */
  String url() default "";

  /**
   * The array of {@code HttpStatus} to retry on
   *
   * @see org.springframework.http.HttpStatus
   */
  HttpStatus[] retryOn() default {HttpStatus.SERVICE_UNAVAILABLE};

  /** The exception to retry on */
  Class<? extends Exception>[] retryOnException() default {IOException.class};
}
