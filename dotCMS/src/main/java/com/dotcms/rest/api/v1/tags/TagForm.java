package com.dotcms.rest.api.v1.tags;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.container.ContainerForm;
import com.dotmarketing.beans.ContainerStructure;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * Form to create a Tag
 * @author Hassan Mustafa Baig
 */
@JsonDeserialize(builder = TagForm.Builder.class)
public class TagForm extends Validated {

    private final String siteId;
    private final String tag;
    private final String site;

    private TagForm(final Builder builder) {

        this.siteId = builder.siteId;
        this.tag = builder.tag;
        this.site = builder. site;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getTag() {
        return tag;
    }

    public String getSite() {
        return site;
    }

    public static final class Builder {
        @JsonProperty
        private String siteId;
        @JsonProperty
        private String tag;
        @JsonProperty
        private String site;

        public Builder siteId (final String siteId) {
            this.siteId = siteId;
            return this;
        }

        public Builder tag (final String tag) {
            this.tag = tag;
            return this;
        }

        public Builder site (final String site) {
            this.site = site;
            return this;
        }

        public TagForm build() {

            return new TagForm(this);
        }
    }
}
