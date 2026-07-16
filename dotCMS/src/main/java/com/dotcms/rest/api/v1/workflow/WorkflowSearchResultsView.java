package com.dotcms.rest.api.v1.workflow;

import java.util.List;

/**
 * View to return a workflow tasks page + total count
 * @author jsanca
 */
public class WorkflowSearchResultsView {

    private final int totalCount;
    private final List<WorkflowTaskView>  workflowTasks;

    public WorkflowSearchResultsView(int totalCount, List<WorkflowTaskView> workflowTasks) {
        this.totalCount = totalCount;
        this.workflowTasks = workflowTasks;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<WorkflowTaskView> getWorkflowTasks() {
        return workflowTasks;
    }
}
