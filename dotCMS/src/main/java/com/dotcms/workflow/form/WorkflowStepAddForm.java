package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowStepAddForm.Builder.class)
public class WorkflowStepAddForm extends Validated implements IWorkflowStepForm{

    //This param is mandatory when creating a new step, but not when updating
    @NotNull
    private final String schemeId;

    @NotNull
    @Length(min = 2, max = 100)
    private final String stepName;

    @NotNull
    private final boolean enableEscalation;

    @NotNull
    private final String escalationAction;

    @NotNull
    private final String escalationTime;

    @NotNull
    private final boolean stepResolved;

    public WorkflowStepAddForm(final Builder builder) {
        this.schemeId = builder.schemeId;
        this.stepName = builder.stepName;
        this.enableEscalation = builder.enableEscalation;
        this.escalationAction = builder.escalationAction;
        this.escalationTime = builder.escalationTime;
        this.stepResolved = builder.stepResolved;
        checkValid();
    }

    public String getSchemeId() {
        return schemeId;
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
        private String schemeId;

        @JsonProperty
        private String stepName;

        @JsonProperty
        private boolean enableEscalation;

        @JsonProperty
        private String escalationAction;

        @JsonProperty
        private String escalationTime;

        @JsonProperty
        private boolean stepResolved;

        public Builder schemeId(final String schemeId) {
            this.schemeId = schemeId;
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

        public WorkflowStepAddForm build() {
            return new WorkflowStepAddForm(this);
        }
    }

}
