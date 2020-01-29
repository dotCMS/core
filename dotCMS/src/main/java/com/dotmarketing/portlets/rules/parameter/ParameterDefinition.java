package com.dotmarketing.portlets.rules.parameter;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.display.Input;
import com.dotmarketing.portlets.rules.parameter.type.DataType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.Nullable;

public class ParameterDefinition<T extends DataType> {

    private final String key;
    private final String i18nBaseKey;
    private final String defaultValue;
    private final Input<T> inputType;
    private final int priority;

    /**
     * Creates a parameter with the key, data type and a default value
     */
    public ParameterDefinition(int priority, String key, Input<T> inputType) {
        this(priority, key, inputType, "");
    }

    public ParameterDefinition(int priority, String key, Input<T> inputType, String defaultValue) {
        this(priority, key, null, inputType, defaultValue);
    }
    public ParameterDefinition(int priority, String key, String i18nBaseKey, Input<T> inputType, String defaultValue) {
        Preconditions.checkState(StringUtils.isNotBlank(key), "ParameterDefinition requires a valid key.");
        this.key = key;
        this.i18nBaseKey = i18nBaseKey;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.inputType = inputType;
        this.priority = priority;
    }

    public String getKey() {
        return key;
    }

    public String getI18nBaseKey() {
        return i18nBaseKey;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public int getPriority() {
        return priority;
    }

    public Input<T> getInputType() {
        return inputType;
    }

    public void checkValid(ParameterModel model) throws InvalidRuleParameterException, RuleEngineException {
    	this.inputType.checkValid(model.getValue());
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
        String key;
        @javax.annotation.Nullable
        String i18nBaseKey;
        @javax.annotation.Nullable
        String defaultValue;
        @javax.annotation.Nullable
        Input inputType;
        @javax.annotation.Nullable
        int priority;

        @JsonProperty("key")
        public void setKey(@Nullable String key) {
            this.key = key;
        }

        @JsonProperty("i18nBaseKey")
        public void setI18nBaseKey(@Nullable String i18nBaseKey) {
            this.i18nBaseKey = i18nBaseKey;
        }

        @JsonProperty("defaultValue")
        public void setDefaultValue(@Nullable String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @JsonProperty("inputType")
        public void setInputType(@Nullable Input inputType) {
            this.inputType = inputType;
        }

        @JsonProperty("priority")
        public void setPriority(@Nullable int priority) {
            this.priority = priority;
        }

    }

    /**
     * @param json A JSON-bindable data structure
     * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    @Deprecated
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ParameterDefinition fromJson(ParameterDefinition.Json json) {
        return new ParameterDefinition(json.priority, json.key, json.i18nBaseKey, json.inputType,
                json.defaultValue);
    }

}