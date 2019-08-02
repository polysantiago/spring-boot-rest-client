package io.github.polysantiago.spring.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.POST)
public @interface PostForLocation {

  /** Alias for {@link RequestMapping#name}. */
  @AliasFor(annotation = RequestMapping.class)
  String name() default "";

  /** Alias for {@link RequestMapping#value}. */
  @AliasFor(annotation = RequestMapping.class)
  String[] value() default {};

  /** Alias for {@link RequestMapping#path}. */
  @AliasFor(annotation = RequestMapping.class)
  String[] path() default {};

  /** Alias for {@link RequestMapping#params}. */
  @AliasFor(annotation = RequestMapping.class)
  String[] params() default {};

  /** Alias for {@link RequestMapping#headers}. */
  @AliasFor(annotation = RequestMapping.class)
  String[] headers() default {};

  /** Alias for {@link RequestMapping#consumes}. */
  @AliasFor(annotation = RequestMapping.class)
  String[] consumes() default {};
}
