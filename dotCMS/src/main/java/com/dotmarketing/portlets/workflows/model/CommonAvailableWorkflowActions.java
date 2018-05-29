package com.dotmarketing.portlets.workflows.model;

import java.util.Collection;
import java.util.Map;

public class CommonAvailableWorkflowActions {

    private final Collection<WorkflowAction> workflowActions;
    private final Map<String, Collection<WorkflowAction>> workflowActionByContentletIdMap;

    public CommonAvailableWorkflowActions(final Collection<WorkflowAction> workflowActions,
                                          final Map<String, Collection<WorkflowAction>> workflowActionByContentletIdMap) {

        this.workflowActions = workflowActions;
        this.workflowActionByContentletIdMap = workflowActionByContentletIdMap;
    }

    public Collection<WorkflowAction> getWorkflowActions() {
        return workflowActions;
    }

    public Map<String, Collection<WorkflowAction>> getWorkflowActionByContentletIdMap() {
        return workflowActionByContentletIdMap;
    }
} // E:O:F:CommonAvailableWorkflowActions.
