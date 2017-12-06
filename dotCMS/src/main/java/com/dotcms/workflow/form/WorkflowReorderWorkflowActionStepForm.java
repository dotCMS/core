package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
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
