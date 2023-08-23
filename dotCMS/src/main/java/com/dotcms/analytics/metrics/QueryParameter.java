package com.dotcms.analytics.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents a Url Query Parameter
 */
public class QueryParameter {
    private final String name;
    private final String value;

    public QueryParameter(@JsonProperty("name")  String name, @JsonProperty("value") String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryParameter that = (QueryParameter) o;
        return Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
