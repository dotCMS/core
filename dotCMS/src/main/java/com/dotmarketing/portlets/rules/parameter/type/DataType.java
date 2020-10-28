package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.portlets.rules.parameter.type.constraint.TypeConstraint;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.Nullable;

/**
 * @author Geoff M. Granum
 */
public abstract class DataType<T> {

    private final String id;
    private final String errorMessageKey;

    private T defaultValue;
    private Map<String, TypeConstraint> restrictions = Maps.newHashMap();

    public DataType(String id, String errorMessageKey) {
        this.id = id;
        this.errorMessageKey = errorMessageKey;
    }

    public DataType(String id, String errorMessageKey, Map<String, TypeConstraint> restrictions) {
        this.id = id;
        this.errorMessageKey = errorMessageKey;
        this.restrictions = restrictions;
    }

    public String getId() {
        return id;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public abstract T convert(String from);

    public T getDefaultValue() {
        return defaultValue;
    }

    public DataType<T> defaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Verify that a value can be represented by this DataType. If valid an empty list will
     * be returned. If invalid a map containing each failed constraint will be returned.
     * @param value The string representation of the value to be verified.
     * @return A map of Errors
     */
    public final Map<String, String> verify(String value){
        Map<String, String> errors = null;
        for (TypeConstraint constraint : restrictions.values()) {
            if(!constraint.fn.apply(value)) {
                if(errors == null) {
                    errors = Maps.newHashMap();
                }
                errors.put(constraint.id, value);
            }
        }
        return errors == null ? Collections.emptyMap() : errors;
    }

    public void checkValid(String value){
        Map<String, String> errors = verify(value);
        if(!errors.isEmpty()){
            String failedMsg = "Constraint(s) failed: [" + StringUtils.join(errors.keySet(), ",") + "]";
            throw new ParameterNotValidException(failedMsg);
        }
    }

    public DataType<T> restrict(TypeConstraint restriction) {
        this.restrictions.put(restriction.id, restriction);
        return this;
    }

    public Map<String, TypeConstraint> getConstraints() {
        return ImmutableMap.copyOf(this.restrictions);
    }

    /**
     * Utility type used to correctly read immutable object from JSON representation.
     *
     * @deprecated Do not use this type directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    @Deprecated
    @JsonDeserialize
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    static final class Json {

        @javax.annotation.Nullable
        String id;
        @javax.annotation.Nullable
        String errorMessageKey;
        @javax.annotation.Nullable
        Map<String, TypeConstraint> restrictions;

        @JsonProperty("id")
        public void setKey(@Nullable final String id) {
            this.id = id;
        }

        @JsonProperty("errorMessageKey")
        public void setDataType(@Nullable final String errorMessageKey) {
            this.errorMessageKey = errorMessageKey;
        }

        @JsonProperty("constraints")
        public void setRestrictions(@Nullable Map<String, TypeConstraint> restrictions) {
            this.restrictions = restrictions;
        }

    }

    /**
     * @param json A JSON-bindable data structure
     * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    @Deprecated
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static DataType fromJson(final DataType.Json json) {
        return new DataType(json.id, json.errorMessageKey, json.restrictions) {
            @Override
            public Object convert(String from) {
                throw new UnsupportedOperationException();
            }
        };
    }

}