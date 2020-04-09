package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

/**
 * Duh its a super class that serves as the base form both Params and Secrets.
 * @param <T>
 */
public abstract class AbstractProperty<T> {

    protected final T value;
    protected final boolean hidden;
    protected final Type type;

    AbstractProperty(final T value, final boolean hidden, final Type type) {
        this.value = value;
        this.hidden = hidden;
        this.type = type;
    }

    public T getValue() {
        return value;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Type getType() {
        return type;
    }

    @JsonIgnore
    public String getString() {
        if(value instanceof char[]){
           return String.valueOf((char[]) value);
        }
        return String.valueOf(value);
    }

    @JsonIgnore
    public boolean getBoolean() {
        return Boolean.parseBoolean(getString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractProperty<?> that = (AbstractProperty<?>) o;
        return hidden == that.hidden &&
                Objects.deepEquals(value, that.value) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, hidden, type);
    }
}
