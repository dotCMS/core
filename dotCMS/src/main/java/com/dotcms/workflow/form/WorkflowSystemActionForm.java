package com.dotcms.workflow.form;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = WorkflowSystemActionForm.Builder.class)
public class WorkflowSystemActionForm extends Validated {

    @NotNull
    private final String actionId;

    private final String schemeId;

    private final String contentTypeVariable;

    @NotNull
    private final WorkflowAPI.SystemAction systemAction;

    public String getActionId() {
        return actionId;
    }

    public String getSchemeId() {
        return schemeId;
    }

    public String getContentTypeVariable() {
        return contentTypeVariable;
    }

    public WorkflowAPI.SystemAction getSystemAction() {
        return systemAction;
    }

    @Override
    public String toString() {
        return "WorkflowSystemActionForm{" +
                "actionId='" + actionId + '\'' +
                ", schemeId='" + schemeId + '\'' +
                ", contentTypeVariable='" + contentTypeVariable + '\'' +
                ", systemAction='" + systemAction + '\'' +
                '}';
    }

    public WorkflowSystemActionForm(final Builder builder) {
        super();
        this.actionId = builder.actionId;
        this.schemeId = builder.schemeId;
        this.contentTypeVariable = builder.contentTypeVariable;
        this.systemAction = builder.systemAction;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String actionId;

        @JsonProperty(required = true)
        private WorkflowAPI.SystemAction systemAction;

        @JsonProperty()
        private String schemeId;

        @JsonProperty()
        private String contentTypeVariable;

        public Builder actionId(final String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder systemAction(final WorkflowAPI.SystemAction systemAction) {
            this.systemAction = systemAction;
            return this;
        }

        public Builder schemeId(final String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        public Builder contentTypeVariable(final String contentTypeVariable) {
            this.contentTypeVariable = contentTypeVariable;
            return this;
        }

        public WorkflowSystemActionForm build() {
            return new WorkflowSystemActionForm(this);
        }
    }
}
