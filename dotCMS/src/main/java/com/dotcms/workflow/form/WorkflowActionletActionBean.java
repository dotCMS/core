package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

import java.util.Map;

@JsonDeserialize(builder = WorkflowActionletActionBean.Builder.class)
public class WorkflowActionletActionBean extends Validated {

    @NotNull
    private final String        actionId;

    @NotNull
    private final String        actionletClass;

    @NotNull
    private final int           order;

    private final Map<String, String> parameters;

    public String getActionId() {
        return actionId;
    }

    public String getActionletClass() {
        return actionletClass;
    }
    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "WorkflowActionletActionBean{" +
                "actionId='" + actionId + '\'' +
                ", actionletClass='" + actionletClass + '\'' +
                ", order=" + order +
                ", parameters=" + parameters +
                '}';
    }

    public WorkflowActionletActionBean(final Builder builder) {

        super();
        this.actionId           = builder.actionId;
        this.actionletClass     = builder.actionletClass;
        this.order              = builder.order;
        this.parameters         = builder.parameters;
        this.checkValid();
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public static final class Builder {

        @JsonProperty()
        private int           order = 0;
        @JsonProperty(required = true)
        private String        actionId;
        @JsonProperty(required = true)
        private String        actionletClass;
        @JsonProperty()
        private Map<String, String> parameters;


        public Builder actionId(final String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder actionletClass(final String actionletClass) {
            this.actionletClass = actionletClass;
            return this;
        }

        public Builder order(final int order) {
            this.order = order;
            return this;
        }

        public Builder parameters(final Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public WorkflowActionletActionBean build() {

            return new WorkflowActionletActionBean(this);
        }
    }
}
