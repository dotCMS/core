package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowReorderBean.Builder.class)
public class WorkflowReorderBean extends Validated {

    @NotNull
    private final String        actionId;

    @NotNull
    private final String        stepId;

    @NotNull
    private final int           order;

    public String getActionId() {
        return actionId;
    }

    public String getStepId() {
        return stepId;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "WorkflowReorderBean{" +
                "actionId='" + actionId + '\'' +
                ", stepId='" + stepId + '\'' +
                ", order=" + order +
                '}';
    }

    public WorkflowReorderBean(final Builder builder) {

        this.actionId           = builder.actionId;
        this.stepId             = builder.stepId;
        this.order              = builder.order;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String        actionId;
        @JsonProperty(required = true)
        private String        stepId;
        @JsonProperty(required = true)
        private int           order;

        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder stepId(String stepId) {
            this.stepId = stepId;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }


        public WorkflowReorderBean build() {

            return new WorkflowReorderBean(this);
        }
    }
}
