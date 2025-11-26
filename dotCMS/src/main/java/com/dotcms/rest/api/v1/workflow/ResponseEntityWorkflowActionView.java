package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;

public class ResponseEntityWorkflowActionView extends ResponseEntityView<WorkflowAction> {
    public ResponseEntityWorkflowActionView(WorkflowAction entity) {
        super(entity);
    }
}
