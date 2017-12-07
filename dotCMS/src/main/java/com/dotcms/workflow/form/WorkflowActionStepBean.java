package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowActionStepBean.Builder.class)
public class WorkflowActionStepBean extends Validated {

    @NotNull
    private final String        actionId;

    @NotNull
    private final String        stepId;

    public String getActionId() {
        return actionId;
    }

    public String getStepId() {
        return stepId;
    }

    @Override
    public String toString() {
        return "WorkflowActionStepBean{" +
                "actionId='" + actionId + '\'' +
                ", stepId='" + stepId + '\'' +
                '}';
    }

    public WorkflowActionStepBean(final Builder builder) {

        this.actionId           = builder.actionId;
        this.stepId             = builder.stepId;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String        actionId;
        @JsonProperty(required = true)
        private String        stepId;


        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder stepId(String stepId) {
            this.stepId = stepId;
            return this;
        }


        public WorkflowActionStepBean build() {

            return new WorkflowActionStepBean(this);
        }
    }
}
