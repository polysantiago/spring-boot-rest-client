package io.github.polysantiago.spring.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RestClientsRegistrar.class)
public @interface EnableRestClients {

  /**
   * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
   * declarations e.g.: {@code @ComponentScan("org.my.pkg")} instead of
   * {@code @ComponentScan(basePackages="org.my.pkg")}.
   *
   * @return the array of 'basePackages'.
   */
  String[] value() default {};

  /**
   * Base packages to scan for annotated components.
   *
   * <p>{@link #value()} is an alias for (and mutually exclusive with) this attribute.
   *
   * <p>Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
   *
   * @return the array of 'basePackages'.
   */
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for
   * annotated components. The package of each class specified will be scanned.
   *
   * <p>Consider creating a special no-op marker class or interface in each package that serves no
   * purpose other than being referenced by this attribute.
   *
   * @return the array of 'basePackageClasses'.
   */
  Class<?>[] basePackageClasses() default {};

  /** List of classes annotated with @RestClient. If not empty, disables classpath scanning. */
  Class<?>[] clients() default {};
}
