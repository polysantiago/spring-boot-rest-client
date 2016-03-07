package se.svt.core.lib.utils.rest;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RestClientsRegistrar.class)
public @interface EnableRestClients {

    /**
     * List of classes annotated with @RestClient. If not empty, disables classpath scanning.
     * @return
     */
    Class<?>[] clients() default {};

}
