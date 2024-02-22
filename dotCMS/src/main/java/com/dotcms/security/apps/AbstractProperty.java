package com.dotcms.security.apps;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This is a super class that serves as the base form both Params and Secrets.
 * @param <T>
 */
public abstract class AbstractProperty<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final T value;
    protected final Boolean hidden;
    protected final Type type;
    protected final String envVar;
    protected final Boolean envShow;
    protected char[] envValue;

    public AbstractProperty(final T value,
                            final Boolean hidden,
                            final Type type,
                            final String envVar,
                            final Boolean envShow) {
        this.value = value;
        this.hidden = hidden;
        this.type = type;
        this.envVar = envVar;
        if (UtilMethods.isSet(this.envVar)) {
            this.envShow = Optional.ofNullable(envShow).orElse(true);
        } else {
            this.envShow = null;
        }
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

    public String getEnvVar() {
        return envVar;
    }

    public Boolean getEnvShow() {
        return envShow;
    }

    public boolean isEnvShow() {
        return UtilMethods.isSet(envShow) ? envShow : true;
    }

    public char[] getEnvValue() {
        return envValue;
    }

    public void setEnvValue(char[] envValue) {
        this.envValue = envValue;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isEditable() {
        return isEnvShow();
    }

    @JsonIgnore
    public String getString() {
        if (isEnvValueSet()) {
            return String.valueOf(envValue);
        }

        return value instanceof char[] ? String.valueOf((char[]) value) : String.valueOf(value);
    }

    @JsonIgnore
    public boolean isEnvVarSet() {
        return UtilMethods.isSet(envVar);
    }

    @JsonIgnore
    public boolean isEnvValueSet() {
        return Objects.nonNull(envValue);
    }

    @JsonIgnore
    public boolean getBoolean() {
        return Boolean.parseBoolean(getString());
    }

    @JsonIgnore
    public List<Map> getList() {
       final List list = (List)value;
       return new ArrayList<>(list);
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
        return Objects.equals(hidden, that.hidden) &&
                Objects.deepEquals(value, that.value) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, hidden, type);
    }
}
