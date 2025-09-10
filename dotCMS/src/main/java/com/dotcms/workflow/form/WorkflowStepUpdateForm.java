package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowStepUpdateForm.Builder.class)
public class WorkflowStepUpdateForm extends Validated implements IWorkflowStepForm {

    @NotNull
    private final Integer stepOrder;

    @NotNull
    @Length(min = 2, max = 100)
    private final String stepName;

    @NotNull
    private final boolean enableEscalation;

    private final String escalationAction;

    @NotNull
    private final String escalationTime;

    @NotNull
    private final boolean stepResolved;

    public WorkflowStepUpdateForm(final Builder builder) {
        this.stepOrder = builder.stepOrder;
        this.stepName = builder.stepName;
        this.enableEscalation = builder.enableEscalation;
        this.escalationAction = builder.escalationAction;
        this.escalationTime = builder.escalationTime;
        this.stepResolved = builder.stepResolved;
        checkValid();
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public boolean isEnableEscalation() {
        return enableEscalation;
    }

    public String getEscalationAction() {
        return escalationAction;
    }

    public String getEscalationTime() {
        return escalationTime;
    }

    public boolean isStepResolved() {
        return stepResolved;
    }

    public static final class Builder {

        @JsonProperty
        private Integer stepOrder;

        @JsonProperty
        private String stepName;

        @JsonProperty
        private boolean enableEscalation;

        @JsonProperty
        private String escalationAction;

        @JsonProperty
        private String escalationTime = "0";

        @JsonProperty
        private boolean stepResolved;

        public Builder stepOrder(final Integer stepOrder) {
            this.stepOrder = stepOrder;
            return this;
        }

        public Builder stepName(final String stepName) {
            this.stepName = stepName;
            return this;
        }

        public Builder enableEscalation(final boolean enableEscalation) {
            this.enableEscalation = enableEscalation;
            return this;
        }

        public Builder escalationAction(final String escalationAction) {
            this.escalationAction = escalationAction;
            return this;
        }

        public Builder escalationTime(final String escalationTime) {
            this.escalationTime = escalationTime;
            return this;
        }

        public Builder stepResolved(final boolean stepResolved) {
            this.stepResolved = stepResolved;
            return this;
        }

        public WorkflowStepUpdateForm build() {
            return new WorkflowStepUpdateForm(this);
        }
    }
}