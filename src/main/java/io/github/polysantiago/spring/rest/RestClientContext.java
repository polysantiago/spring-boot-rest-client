package io.github.polysantiago.spring.rest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class RestClientContext {

  private List<RestClientSpecification> specifications;
  private Map<String, Object> services;

  RestClientSpecification findByRestClientName(String name) {
    return specifications.stream()
        .filter(specification -> StringUtils.equals(specification.getName(), name))
        .findAny()
        .orElseThrow(
            () -> new IllegalStateException("Unable to find a @RestClient with name: " + name));
  }

  URI findServiceUriByName(String name) {
    return Optional.ofNullable(services.get(name))
        .map(Object::toString)
        .map(URI::create)
        .orElseThrow(() -> new IllegalStateException("Invalid URL for service " + name));
  }
}
