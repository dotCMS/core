package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

}
