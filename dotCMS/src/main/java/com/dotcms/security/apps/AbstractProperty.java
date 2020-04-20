package com.dotcms.security.apps;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

/**
 * its a super class that serves as the base form both Params and Secrets.
 * @param <T>
 */
public abstract class AbstractProperty<T> {

    protected final T value;
    protected final Boolean hidden;
    protected final Type type;

    AbstractProperty(final T value, final Boolean hidden, final Type type) {
        this.value = value;
        this.hidden = hidden;
        this.type = type;
    }

    public T getValue() {
        return value;
    }

    public boolean isHidden() {
        return UtilMethods.isSet(hidden) ? hidden : false;
    }

    public Boolean getHidden() {
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
        return  Objects.equals(hidden, that.hidden) &&
                Objects.deepEquals(value, that.value) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, hidden, type);
    }
}
