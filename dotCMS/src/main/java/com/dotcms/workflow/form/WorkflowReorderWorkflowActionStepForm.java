package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowReorderWorkflowActionStepForm.Builder.class)
public class WorkflowReorderWorkflowActionStepForm extends Validated {


    @NotNull
    private final int           order;

    public int getOrder() {
        return order;
    }


    public WorkflowReorderWorkflowActionStepForm(final Builder builder) {

        this.order              = builder.order;
        this.checkValid();
    }

    @Override
    public String toString() {
        return "WorkflowReorderWorkflowActionStepForm{" +
                "order=" + order +
                '}';
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private int           order;

        public Builder order(int order) {
            this.order = order;
            return this;
        }


        public WorkflowReorderWorkflowActionStepForm build() {

            return new WorkflowReorderWorkflowActionStepForm(this);
        }
    }
}
