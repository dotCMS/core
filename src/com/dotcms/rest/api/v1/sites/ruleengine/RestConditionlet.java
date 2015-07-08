package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

@JsonDeserialize(builder = RestConditionlet.Builder.class)
public final class RestConditionlet {

    public final String id;
    public final String name;
    public final List<RestComparison> comparisons;

    private RestConditionlet(Builder builder) {
        id = builder.id;
        name = builder.name;
        comparisons = ImmutableList.copyOf(builder.comparisons);
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String name;
        @JsonProperty private List<RestComparison> comparisons = Collections.emptyList();

        /*
            RestConditionlet restConditionlet = new RestConditionlet.Builder()
            .id( input.getId() )
            .name( input.getName() )
            .comparisons( input.getComparisons() )
            .build();
        */
        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder comparisons(List<RestComparison> comparisons) {
            this.comparisons = comparisons;
            return this;
        }

        public Builder from(RestConditionlet copy) {
            id = copy.id;
            name = copy.name;
            comparisons = copy.comparisons;
            return this;
        }

        public RestConditionlet build() {
            return new RestConditionlet(this);
        }
    }
}

