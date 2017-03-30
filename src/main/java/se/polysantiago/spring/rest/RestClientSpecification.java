package se.polysantiago.spring.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestClientSpecification {

    private String name;
    private HttpStatus[] retryableStatuses;
    private Class<? extends Exception>[] retryableExceptions;

}
