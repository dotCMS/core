package com.dotcms.rest.api.v1.system.ruleengine.conditionlets;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.validation.constraints.Size;
//import com.dotcms.repackage.org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.system.ruleengine.actionlets.RestComparison;

import java.util.List;

@JsonDeserialize(builder = RestConditionlet.Builder.class)
public final class RestConditionlet extends Validated {

    public final String id;

    @NotNull
    public final String i18nKey;

    @NotNull
    @Size(min = 0, max = 100)
    public final List<RestComparison> comparisons;

    private RestConditionlet(Builder builder) {
        id = builder.id;
        i18nKey = builder.i18nKey;
        comparisons = ImmutableList.copyOf(builder.comparisons);
        checkValid();
    }

    public static final class Builder  {

        private String id;
        private String i18nKey;
        private List<RestComparison> comparisons;

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

        public Builder comparisons(List<RestComparison> comparisons) {
            this.comparisons = comparisons;
            return this;
        }

        public Builder from(RestConditionlet copy) {
            id = copy.id;
            i18nKey = copy.i18nKey;
            comparisons = copy.comparisons;
            return this;
        }

        public RestConditionlet build() {
            return  new RestConditionlet(this);
        }
    }
}

