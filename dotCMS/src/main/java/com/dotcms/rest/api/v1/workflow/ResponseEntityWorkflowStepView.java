package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

public class ResponseEntityWorkflowStepView extends ResponseEntityView<WorkflowStep> {
    public ResponseEntityWorkflowStepView(WorkflowStep entity) {
        super(entity);
    }
}
