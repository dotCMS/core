package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UpdateTagForm.Builder.class)
public class UpdateTagForm {

    @JsonIgnore
    public final String siteId;
    public final String tagName;
    public final String tagId;

    public UpdateTagForm(final Builder builder) {
        this.siteId = builder.siteId;
        this.tagName = builder.tagName;
        this.tagId = builder.tagId;
    }

    public static final class Builder {

        @JsonProperty
        private String siteId;
        @JsonProperty
        private String tagName;
        @JsonProperty
        private String tagId;

        public Builder() {
        }

        Builder siteId(final String siteId) {
            this.siteId = siteId;
            return this;
        }

        Builder tagName(final String tagName) {
            this.tagName = tagName;
            return this;
        }

        Builder tagId(final String tagId) {
            this.tagId = tagId;
            return this;
        }

    }
}
