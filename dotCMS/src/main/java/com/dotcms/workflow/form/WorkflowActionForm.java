package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.portlets.workflows.model.WorkflowStatus;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@JsonDeserialize(builder = WorkflowActionForm.Builder.class)
public class WorkflowActionForm extends Validated {

    private final String        actionId;

    @NotNull
    private final String        schemeId;

    // you can send an optional stepId for a new Action when you want to associated the action to the step in the same transaction.
    private final String        stepId;

    @NotNull
    private final String        actionName;
    private final List<String>  whoCanUse;
    private final String        actionIcon;
    @NotNull
    private final boolean       actionAssignable;
    @NotNull
    private final boolean       actionCommentable;
    @NotNull
    private final boolean       requiresCheckout;
    @NotNull
    private final Set<WorkflowStatus> showOn;
    @NotNull
    private final boolean       actionRoleHierarchyForAssign;
    private final boolean       roleHierarchyForAssign;
    @NotNull
    private final String        actionNextStep;
    private final String        actionNextAssign;
    private final String        actionCondition;

    public String getStepId() {
        return stepId;
    }

    public String getActionId() {
        return actionId;
    }

    public String getSchemeId() {
        return schemeId;
    }

    public String getActionName() {
        return actionName;
    }

    public List<String> getWhoCanUse() {
        return whoCanUse;
    }

    public String getActionIcon() {
        return actionIcon;
    }

    public boolean isActionAssignable() {
        return actionAssignable;
    }

    public boolean isActionCommentable() {
        return actionCommentable;
    }

    public boolean isRequiresCheckout() {
        return requiresCheckout;
    }

    public Set<WorkflowStatus> getShowOn() {
        return Collections.unmodifiableSet(showOn);
    }

    public boolean isActionRoleHierarchyForAssign() {
        return actionRoleHierarchyForAssign;
    }

    public boolean isRoleHierarchyForAssign() {
        return roleHierarchyForAssign;
    }

    public String getActionNextStep() {
        return actionNextStep;
    }

    public String getActionNextAssign() {
        return actionNextAssign;
    }

    public String getActionCondition() {
        return actionCondition;
    }

    @Override
    public String toString() {
        return "WorkflowActionForm{" +
                "actionId='" + actionId + '\'' +
                ", schemeId='" + schemeId + '\'' +
                ", stepId='" + stepId + '\'' +
                ", actionName='" + actionName + '\'' +
                ", whoCanUse=" + whoCanUse +
                ", actionIcon='" + actionIcon + '\'' +
                ", actionAssignable=" + actionAssignable +
                ", actionCommentable=" + actionCommentable +
                ", requiresCheckout=" + requiresCheckout +
                ", showOn=" + showOn +
                ", actionRoleHierarchyForAssign=" + actionRoleHierarchyForAssign +
                ", roleHierarchyForAssign=" + roleHierarchyForAssign +
                ", actionNextStep='" + actionNextStep + '\'' +
                ", actionNextAssign='" + actionNextAssign + '\'' +
                ", actionCondition='" + actionCondition + '\'' +
                '}';
    }

    public WorkflowActionForm(final Builder builder) {

        this.actionId                   = builder.actionId;
        this.schemeId                   = builder.schemeId;
        this.stepId                     = builder.stepId;
        this.actionName                 = builder.actionName;
        this.whoCanUse                  = builder.whoCanUse;
        this.actionIcon                 = builder.actionIcon;
        this.actionCommentable          = builder.actionCommentable;
        this.requiresCheckout           = builder.requiresCheckout;
        this.showOn                     = builder.showOn;
        this.actionNextStep             = builder.actionNextStep;
        this.actionNextAssign           = builder.actionNextAssign;
        this.actionCondition            = builder.actionCondition;
        this.actionAssignable           = builder.actionAssignable;
        this.actionRoleHierarchyForAssign = builder.actionRoleHierarchyForAssign;
        this.roleHierarchyForAssign     = (actionAssignable && actionRoleHierarchyForAssign);
        this.checkValid();
    }

       public static final class Builder {

        @JsonProperty()
        private String        actionId;

        @JsonProperty()
        private String        stepId;

        @JsonProperty(required = true)
        private String schemeId;

        @JsonProperty(required = true)
        private String        actionName;
        @JsonProperty()
        private List<String>  whoCanUse;
        @JsonProperty()
        private String        actionIcon;
        @JsonProperty(required = true)
        private boolean       actionAssignable;
        @JsonProperty(required = true)
        private boolean       actionCommentable;
        @JsonProperty(required = true)
        private boolean       requiresCheckout;
        @JsonProperty(required = true)
        private boolean       actionRoleHierarchyForAssign;
        @JsonProperty(required = true)
        private String        actionNextStep;
        @JsonProperty()
        private String        actionNextAssign;
        @JsonProperty()
        private String        actionCondition;

        @JsonProperty(required = true)
        private Set<WorkflowStatus> showOn;


       public Builder showOn(Set<WorkflowStatus> showOn) {
           this.showOn = showOn;
           return this;
       }

        public Builder stepId(String stepId) {
            this.stepId = stepId;
            return this;
        }

        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder schemeId(String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        public Builder actionName(String actionName) {
            this.actionName = actionName;
            return this;
        }

        public Builder whoCanUse(List<String> whoCanUse) {
            this.whoCanUse = whoCanUse;
            return this;
        }

        public Builder actionIcon(String actionIcon) {
            this.actionIcon = actionIcon;
            return this;
        }

        public Builder actionAssignable(boolean actionAssignable) {
            this.actionAssignable = actionAssignable;
            return this;
        }

        public Builder actionCommentable(boolean actionCommentable) {
            this.actionCommentable = actionCommentable;
            return this;
        }

        public Builder requiresCheckout(boolean requiresCheckout) {
            this.requiresCheckout = requiresCheckout;
            return this;
        }

        public Builder actionRoleHierarchyForAssign(boolean actionRoleHierarchyForAssign) {
            this.actionRoleHierarchyForAssign = actionRoleHierarchyForAssign;
            return this;
        }

        public Builder actionNextStep(String actionNextStep) {
            this.actionNextStep = actionNextStep;
            return this;
        }

        public Builder actionNextAssign(String actionNextAssign) {
            this.actionNextAssign = actionNextAssign;
            return this;
        }

        public Builder actionCondition(String actionCondition) {
            this.actionCondition = actionCondition;
            return this;
        }

        public WorkflowActionForm build() {
            return new WorkflowActionForm(this);
        }
    }
}
