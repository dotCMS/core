package com.dotcms.rest.api.v1.system.ruleengine.conditionlets;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.validation.constraints.Size;
import com.dotcms.rest.api.Validated;

import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@JsonDeserialize(builder = RestConditionlet.Builder.class)
public final class RestConditionlet extends Validated {

    public final String id;

    @NotNull
    public final String i18nKey;

    @NotNull
    @Size(min = 0, max = 100)
    public final Map<String, ParameterDefinition> parameterDefinitions;

    private RestConditionlet(Builder builder) {
        id = builder.id;
        i18nKey = builder.i18nKey;
        parameterDefinitions = ImmutableMap.copyOf(builder.parameterDefinitions);
        checkValid();
    }

    public static final class Builder  {

        private String id;
        private String i18nKey;
        private Map<String, ParameterDefinition> parameterDefinitions;

        public Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder i18nKey(String i18nKey) {
            this.i18nKey = i18nKey;
            return this;
        }

        public Builder parameters(Map<String, ParameterDefinition> comparisons) {
            this.parameterDefinitions = comparisons;
            return this;
        }

        public Builder from(RestConditionlet copy) {
            id = copy.id;
            i18nKey = copy.i18nKey;
            parameterDefinitions = copy.parameterDefinitions;
            return this;
        }

        public RestConditionlet build() {
            return  new RestConditionlet(this);
        }
    }
}

