package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

import java.util.Map;

@JsonDeserialize(builder = WorkflowActionletActionForm.Builder.class)
public class WorkflowActionletActionForm extends Validated {

    @NotNull
    private final String        actionletClass;

    private final int           order;

    private final Map<String, String> parameters;

    public String getActionletClass() {
        return actionletClass;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public int getOrder() {
        return order;
    }

    public WorkflowActionletActionForm(final Builder builder) {

        super();
        this.actionletClass = builder.actionletClass;
        this.order          = builder.order;
        this.parameters     = builder.parameters;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty()
        private int order;

        @JsonProperty(required = true)
        private String        actionletClass;

        @JsonProperty()
        private Map<String, String> parameters;


        public Builder actionletClass(final String actionletClass) {
            this.actionletClass = actionletClass;
            return this;
        }

        public Builder parameters(final Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder order(final int order) {
            if (order > 0) {
                this.order = order;
            }
            return this;
        }

        public WorkflowActionletActionForm build() {

            return new WorkflowActionletActionForm(this);
        }
    }

    @Override
    public String toString() {
        return "WorkflowActionletActionForm{" +
                ", actionletClass='" + actionletClass + '\'' +
                ", order=" + order +
                ", parameters=" + parameters +
                '}';
    }
}