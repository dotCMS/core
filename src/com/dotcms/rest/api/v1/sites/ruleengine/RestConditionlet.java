package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonDeserialize(builder = RestConditionlet.Builder.class)
public final class RestConditionlet {

    public final String id;
    public final String localizedName;
    public final String languageId;
    public final List<RestComparison> comparisons;

    private RestConditionlet(Builder builder) {
        id = builder.id;
        localizedName = builder.localizedName;
        languageId = builder.languageId;
        comparisons = builder.comparisons;
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String localizedName;
        @JsonProperty private String languageId;
        @JsonProperty private List<RestComparison> comparisons;

        /*
            RestConditionlet restConditionlet = new RestConditionlet.Builder()
            .id( input.getId() )
            .name( input.getName() )
            .localizedName( input.getLocalizedName() )
            .languageId( input.getLanguageId() )
            .comparisons( input.getComparisons() )
            .build();
        */
        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }


        public Builder localizedName(String localizedName) {
            this.localizedName = localizedName;
            return this;
        }

        public Builder languageId(String languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder comparisons(List<RestComparison> comparisons) {
            this.comparisons = comparisons;
            return this;
        }

        public Builder from(RestConditionlet copy) {
            id = copy.id;
            localizedName = copy.localizedName;
            languageId = copy.languageId;
            comparisons = copy.comparisons;
            return this;
        }

        public RestConditionlet build() {
            return new RestConditionlet(this);
        }
    }
}

