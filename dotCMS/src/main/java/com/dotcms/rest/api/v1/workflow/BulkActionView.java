package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

import java.util.List;
import java.util.Map;

public class BulkActionView {

    private final Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> bulkActions;

    public BulkActionView(final Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> bulkActions) {
        this.bulkActions   = bulkActions;
    }

    public Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> getBulkActions() {
        return bulkActions;
    }
}
