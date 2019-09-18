package com.dotmarketing.portlets.workflows.model;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Encapsulates the mapping between {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} to {@link WorkflowAction}
 * for a {@link ContentType} or {@link WorkflowScheme}
 * @author jsanca
 */
@JsonDeserialize(using = SystemActionWorkflowActionMappingDeserializer.class)
public class SystemActionWorkflowActionMapping {

    private final String identifier;
    private final WorkflowAPI.SystemAction systemAction;
    private final WorkflowAction workflowAction;
    private final Object owner; // the owner could be a ContentType or WorkflowScheme

    @JsonCreator
    public SystemActionWorkflowActionMapping(@JsonProperty("identifier")  final String identifier,
                                             @JsonProperty("systemAction")  final WorkflowAPI.SystemAction systemAction,
                                             @JsonProperty("workflowAction")  final WorkflowAction workflowAction,
                                             @JsonProperty("owner")  final Object owner) {
        this.identifier = identifier;
        this.systemAction = systemAction;
        this.workflowAction = workflowAction;
        this.owner = owner;
    }

    public String getIdentifier() {
        return identifier;
    }

    public WorkflowAPI.SystemAction getSystemAction() {
        return systemAction;
    }

    public WorkflowAction getWorkflowAction() {
        return workflowAction;
    }

    public Object getOwner() {
        return owner;
    }

    public boolean isOwnerContentType () {

        return owner instanceof ContentType;
    }

    public boolean isOwnerScheme () {

        return owner instanceof WorkflowScheme;
    }



    @Override
    public String toString() {
        return "SystemActionWorkflowActionMapping{" +
                "identifier='" + identifier + '\'' +
                ", systemAction=" + systemAction +
                ", workflowAction=" + workflowAction +
                ", owner=" + owner +
                '}';
    }
}
