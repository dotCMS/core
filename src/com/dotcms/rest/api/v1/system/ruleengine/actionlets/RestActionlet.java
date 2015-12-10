package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import java.util.Map;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = RestActionlet.Builder.class)
public final class RestActionlet extends Validated {

    public final String id;

    public final String i18nKey;

    public final Map<String, ParameterDefinition> parameterDefinitions;

    private RestActionlet(Builder builder) {
        id = builder.id;
        i18nKey = builder.i18nKey;
        parameterDefinitions = ImmutableMap.copyOf(builder.parameters);
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String i18nKey;
        @JsonProperty private Map<String, ParameterDefinition> parameters;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder parameters(Map<String, ParameterDefinition> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder i18nKey(String i18nKey) {
            this.i18nKey = i18nKey;
            return this;
        }

        public Builder from(RestActionlet copy) {
            id = copy.id;
            i18nKey = copy.i18nKey;
            parameters = copy.parameterDefinitions;
            return this;
        }

        public RestActionlet build() {
            return new RestActionlet(this);
        }
    }
}

