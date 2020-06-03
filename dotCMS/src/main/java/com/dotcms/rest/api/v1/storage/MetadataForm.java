package com.dotcms.rest.api.v1.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = MetadataForm.Builder.class)
public class MetadataForm {

    private final String  field;
    private final boolean cache;

    public MetadataForm(final Builder builder) {

        this.field      = builder.field;
        this.cache      = builder.cache;
    }


    public String getField() {
        return field;
    }

    public boolean isCache() {
        return cache;
    }



    public static final class Builder {

        @JsonProperty private String  field;
        @JsonProperty private boolean cache = true;

        public Builder cache(final boolean cache) {
            this.cache = cache;
            return this;
        }


        public Builder field(final String field) {
            this.field = field;
            return this;
        }

        public MetadataForm build() {
            return new MetadataForm(this);
        }
    }
}
