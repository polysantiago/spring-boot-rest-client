package io.github.polysantiago.spring.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.ResourceSupport;

@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class FooResource extends ResourceSupport {

  private final String bar;
}
