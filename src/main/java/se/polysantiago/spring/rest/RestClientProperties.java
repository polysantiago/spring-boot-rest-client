package se.polysantiago.spring.rest;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import se.polysantiago.spring.rest.retry.RetrySettings;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@Data
@ConfigurationProperties(prefix = RestClientProperties.PREFIX)
public class RestClientProperties {

    public static final String PREFIX = "spring.rest.client";

    private Boolean isoDateTimeFormat = true;
    private RetrySettings retry = new RetrySettings();
    private Map<String, Object> services = newHashMap();

}
