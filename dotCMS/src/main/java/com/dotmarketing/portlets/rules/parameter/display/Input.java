package com.dotmarketing.portlets.rules.parameter.display;

import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.parameter.type.DataType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.elasticsearch.common.Nullable;

/**
 * @author Geoff M. Granum
 */
public class Input<T extends DataType> {

    private final String id;
    private final T dataType;

    public Input(String id, T dataType) {
        this.id = id;
        this.dataType = dataType;
    }

    public String getId() {
        return id;
    }

    public T getDataType() {
        return dataType;
    }

    /**
     * Validates the parameter context for the conditionlet. Each input will implement this validation if required.
     * @param value parameter value
     * @throws InvalidRuleParameterException
     */
    public void checkValid(String value)  throws InvalidRuleParameterException{
    	return;
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
        DataType dataType;

        @JsonProperty("id")
        public void setKey(@Nullable String id) {
            this.id = id;
        }

        @JsonProperty("dataType")
        public void setDataType(@Nullable DataType dataType) {
            this.dataType = dataType;
        }

    }

    /**
     * @param json A JSON-bindable data structure
     * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    @Deprecated
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Input fromJson(Input.Json json) {
        return new Input(json.id, json.dataType);
    }

}