package com.dotcms.workflow.form;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.Hidden;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a Workflow Action Form used by different parts of the system, such as REST
 * Endpoints. It is used to create, update and delete Workflow Actions in dotCMS.
 *
 * @author Jonathan Sanchez
 * @since Dec 6th, 2017
 */
@JsonDeserialize(builder = WorkflowActionForm.Builder.class)
public class WorkflowActionForm extends Validated {

    private final String        actionId;

    @NotNull
    private final String        schemeId;

    // You can send an optional stepId for a new Action when you want to associate it to the step in the same transaction.
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
    @Hidden
    private final boolean       requiresCheckout;
    @NotNull
    private final Set<WorkflowState> showOn;
    @NotNull
    private final boolean       actionRoleHierarchyForAssign;
    @Hidden
    private final boolean       roleHierarchyForAssign;
    @NotNull
    private final String        actionNextStep;
    private final String        actionNextAssign;
    private final String        actionCondition;
    private static final String METADATA_SUBTYPE_ATTR = "subtype";
    private final Map<String, Object> metadata;

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

    public Set<WorkflowState> getShowOn() {
        return (null != showOn)?Collections.unmodifiableSet(showOn):Collections.emptySet();
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

    public Map<String, Object> getMetadata() {
        return this.metadata;
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
                ", metadata='" + metadata + '\'' +
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
        this.metadata = builder.metadata;
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

       /**
        * @deprecated This attribute is not necessary as a single workflow action can be available
        * for locked and/or unlocked content now. See
        * <a href="https://github.com/dotCMS/core/issues/13287">#13287</a>
        */
        @Deprecated
        @JsonProperty(required = true)
        private boolean       requiresCheckout = false;
        @JsonProperty(required = true)
        private boolean       actionRoleHierarchyForAssign;
        @JsonProperty(required = true)
        private String        actionNextStep;
        @JsonProperty()
        private String        actionNextAssign;
        @JsonProperty()
        private String        actionCondition;

        @JsonProperty(required = true)
        private Set<WorkflowState> showOn;
        @JsonProperty()
        private Map<String, Object> metadata;

       public Builder showOn(Set<WorkflowState> showOn) {
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

        @Deprecated
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

        /**
         * Sets the metadata for this Workflow Action. This is a Map of key/value pairs that may
         * include different custom properties that define the behavior of an action.
         *
         * @param metadata Different custom properties for this action.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder metadata(final Map<String, Object> metadata) {
           this.metadata = metadata;
           return this;
        }

       /**
        * Marks this Workflow Action as a Separator. This is a special type of action that does
        * not execute any sub-actions at all, as it simply groups X number of actions together
        * in the UI. The result of this may be seen as the differentiation between Primary and
        * Secondary Actions.
        *
        * @param schemeId The ID of the Workflow Scheme that this action belongs to.
        * @param stepId   The ID of the Workflow Step that this action belongs to.
        *
        * @return The current {@link Builder} instance.
        */
        public Builder separator(final String schemeId, final String stepId) {
            this.schemeId(schemeId);
            this.stepId(stepId);
            this.actionName(WorkflowAction.SEPARATOR);
            this.actionAssignable(false);
            this.actionCommentable(false);
            this.actionRoleHierarchyForAssign(false);
            this.actionNextStep(WorkflowAction.CURRENT_STEP);
            this.actionNextAssign(Try.of(() -> APILocator.getRoleAPI().loadRoleByKey(Role.CMS_ANONYMOUS_ROLE).getId())
                    .getOrElseThrow(e -> new DotRuntimeException("Anonymous Role ID not found in the database", e)));
            this.actionCondition(WorkflowAction.SEPARATOR);
            this.showOn(Arrays.stream(WorkflowState.values()).filter(state -> state != WorkflowState.LISTING).collect(java.util.stream.Collectors.toSet()));
            if (null == this.metadata) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(METADATA_SUBTYPE_ATTR, WorkflowAction.SEPARATOR);
            return this;
        }

        public WorkflowActionForm build() {
            return new WorkflowActionForm(this);
        }

    }

}
