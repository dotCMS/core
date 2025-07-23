package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowActionStepForm.Builder.class)
public class WorkflowActionStepForm extends Validated {

    @NotNull
    private final String        actionId;

    public String getActionId() {
        return actionId;
    }


    public WorkflowActionStepForm(final Builder builder) {

        this.actionId           = builder.actionId;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String        actionId;


        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }


        public WorkflowActionStepForm build() {

            return new WorkflowActionStepForm(this);
        }
    }

    @Override
    public String toString() {
        return "WorkflowActionStepForm{" +
                "actionId='" + actionId + '\'' +
                '}';
    }
}