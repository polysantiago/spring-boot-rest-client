package io.github.polysantiago.spring.rest;

import io.github.polysantiago.spring.rest.retry.RetrySettings;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = RestClientProperties.PREFIX)
public class RestClientProperties {

    public static final String PREFIX = "spring.rest.client";

    private Boolean isoDateTimeFormat = true;
    private RetrySettings retry = new RetrySettings();
    private Map<String, Object> services = new HashMap<>();

}
