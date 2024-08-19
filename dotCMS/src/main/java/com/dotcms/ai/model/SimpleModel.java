package com.dotcms.ai.model;

import com.dotcms.ai.app.AIModelType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a simple model with a name and type.
 * This class is immutable and uses Jackson annotations for JSON serialization and deserialization.
 *
 * @author vico
 */
public class SimpleModel implements Serializable {

    private final String name;
    private final AIModelType type;

    @JsonCreator
    public SimpleModel(@JsonProperty("name") final String name, @JsonProperty("type") final AIModelType type) {
        this.name = name;
        this.type = type;
    }

    @JsonCreator
    public SimpleModel(@JsonProperty("name") final String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public AIModelType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleModel that = (SimpleModel) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

}
