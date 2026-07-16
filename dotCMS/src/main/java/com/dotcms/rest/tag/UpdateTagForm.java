package com.dotcms.rest.tag;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonDeserialize(builder = UpdateTagForm.Builder.class)
public class UpdateTagForm extends Validated {

    @NotNull
    public final String siteId;

    @NotNull
    public final String tagName;

    @NotNull
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

        UpdateTagForm build(){
            return new UpdateTagForm(this);
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

    /**
     * good old toString
     * @return
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }

}
