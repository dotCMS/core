package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

import java.util.List;

public class ResponseEntityWorkflowStepsView extends ResponseEntityView<List<WorkflowStep>> {
    public ResponseEntityWorkflowStepsView(List<WorkflowStep> steps) {
        super(steps);
    }
}
