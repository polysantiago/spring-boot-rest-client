package se.svt.core.lib.utils.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Foo {

    private final String bar;

    @JsonCreator
    public Foo(@JsonProperty("bar") String bar) {
        this.bar = bar;
    }

    public String getBar() {
        return bar;
    }
}
