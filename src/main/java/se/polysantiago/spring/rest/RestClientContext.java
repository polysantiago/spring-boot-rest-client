package se.polysantiago.spring.rest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.*;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class RestClientContext {

    private List<RestClientSpecification> specifications = new ArrayList<>();
    private Map<String, Object> services = new HashMap<>();

    RestClientSpecification findByRestClientName(String name) {
        return specifications
            .stream()
            .filter(specification -> StringUtils.equals(specification.getName(), name))
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Unable to find a @RestClient with name: " + name));
    }

    URI findServiceUriByName(String name) {
        return Optional.ofNullable(services.get(name))
            .map(Object::toString)
            .map(URI::create)
            .orElseThrow(() -> new IllegalStateException("Invalid URL for service " + name));
    }
}
