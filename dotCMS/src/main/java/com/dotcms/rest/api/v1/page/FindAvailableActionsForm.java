package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Encapsulates the values for the page check permission
 * @author jsanca
 */
@JsonDeserialize(builder = FindAvailableActionsForm.Builder.class)
public class FindAvailableActionsForm extends Validated {

    private WorkflowAPI.RenderMode renderMode;
    private final String hostId;
    private final long languageId;
    private final String path;

    public FindAvailableActionsForm(final Builder builder) {

        this.renderMode = builder.renderMode;
        this.hostId = builder.hostId;
        this.languageId = builder.languageId;
        this.path = builder.path;
        this.checkValid();
    }

    public WorkflowAPI.RenderMode getRenderMode() {
        return renderMode;
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
        private WorkflowAPI.RenderMode renderMode = WorkflowAPI.RenderMode.LISTING;

        @JsonProperty(value = "host_id")
        private String hostId;
        @JsonProperty(value = "language_id")
        private long languageId = -1;
        @JsonProperty(value = "url",required = true)
        private String path;

        public Builder renderMode(final WorkflowAPI.RenderMode renderMode) {
            this.renderMode = renderMode;
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

        public FindAvailableActionsForm build() {
            return new FindAvailableActionsForm(this);
        }
    }
}
