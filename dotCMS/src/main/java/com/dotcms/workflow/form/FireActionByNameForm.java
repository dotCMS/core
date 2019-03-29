package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = FireActionByNameForm.Builder.class)
public class FireActionByNameForm extends FireActionForm {

    private final String actionName;

    public FireActionByNameForm(final Builder builder) {

        super(builder);
        this.actionName = builder.actionName;
    }

    public String getActionName() {
        return actionName;
    }

    public static final class Builder extends FireActionForm.Builder {

        @JsonProperty()
        private String actionName;

        public Builder actionName(final String actionName) {
            this.actionName = actionName;
            return this;
        }

        public FireActionByNameForm build() {
            return new FireActionByNameForm(this);
        }
    }
}
