package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = WorkflowSchemeForm.Builder.class)
public class WorkflowSchemeForm extends Validated {

    @NotNull
    @Length(min = 2, max = 100)
    private final String schemeName;

    private final String schemeDescription;

    private final boolean schemeArchived;

    public WorkflowSchemeForm(final WorkflowSchemeForm.Builder builder) {
        this.schemeName = builder.schemeName;
        this.schemeDescription = builder.schemeDescription;
        this.schemeArchived = builder.schemeArchived;
        this.checkValid();
    }

    public String getSchemeName() {
        return schemeName;
    }

    public String getSchemeDescription() {
        return schemeDescription;
    }

    public boolean isSchemeArchived() {
        return schemeArchived;
    }

    public static final class Builder {

        @JsonProperty
        private String schemeName;

        @JsonProperty
        private String schemeDescription;

        @JsonProperty
        private boolean schemeArchived;

        public Builder schemeName(final String schemeName) {
            this.schemeName = schemeName;
            return this;
        }

        public Builder schemeDescription(final String schemeDescription) {
            this.schemeDescription = schemeDescription;
            return this;
        }

        public Builder schemeArchived(final boolean schemeArchived) {
            this.schemeArchived = schemeArchived;
            return this;
        }

        public WorkflowSchemeForm build() {
            return new WorkflowSchemeForm(this);
        }

    }
}
