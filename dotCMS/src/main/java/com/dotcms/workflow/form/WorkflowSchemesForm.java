package com.dotcms.workflow.form;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Set;

@JsonDeserialize(builder = WorkflowSchemesForm.Builder.class)
public class WorkflowSchemesForm extends Validated {

    @NotNull
    private final Set<String>        schemes;

    public Set<String> getSchemes() {
        return schemes;
    }

    public WorkflowSchemesForm(final Builder builder) {

        super();
        this.schemes           = builder.schemes;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private Set<String>        schemes;


        public Builder schemes(final Set<String>  schemes) {
            this.schemes = schemes;
            return this;
        }


        public WorkflowSchemesForm build() {

            return new WorkflowSchemesForm(this);
        }
    }

    @Override
    public String toString() {
        return "WorkflowSchemesForm{" +
                "schemes=" + schemes +
                '}';
    }
}