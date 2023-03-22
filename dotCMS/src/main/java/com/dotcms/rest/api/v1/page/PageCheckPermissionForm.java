package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.business.PermissionLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Encapsulates the values for the page check permission
 * @author jsanca
 */
@JsonDeserialize(builder = PageCheckPermissionForm.Builder.class)
public class PageCheckPermissionForm extends Validated {

    private final PermissionLevel type;
    private final String hostId;
    private final long languageId;
    private final String path;

    public PageCheckPermissionForm(final Builder builder) {

        this.type = builder.type;
        this.hostId = builder.hostId;
        this.languageId = builder.languageId;
        this.path = builder.path;
        this.checkValid();
    }

    public PermissionLevel getType() {
        return type;
    }

    public String getHostId() {
        return hostId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getPath() {
        return path;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder  {

        @JsonProperty()
        private PermissionLevel type = PermissionLevel.READ;

        @JsonProperty(value = "host_id")
        private String hostId;
        @JsonProperty(value = "language_id")
        private long languageId = -1;
        @JsonProperty(value = "url",required = true)
        private String path;

        public Builder type(final PermissionLevel type) {
            this.type = type;
            return this;
        }

        public Builder hostId(final String hostId) {
            this.hostId = hostId;
            return this;
        }

        public Builder languageId(final long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public PageCheckPermissionForm build() {
            return new PageCheckPermissionForm(this);
        }
    }
}
