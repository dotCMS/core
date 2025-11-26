package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowCopyForm.Builder.class)
public class WorkflowCopyForm extends Validated {

    @Length(min = 2, max = 100)
    private final String name;

    public WorkflowCopyForm(final WorkflowCopyForm.Builder builder) {
      this.name = builder.name;
      checkValid();
    }

    public String getName() {
        return name;
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String name;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public WorkflowCopyForm build() {
            return new WorkflowCopyForm(this);
        }
    }

}
