package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;


public class ResponseEntityWorkflowSchemeView extends ResponseEntityView<WorkflowScheme> {
    public ResponseEntityWorkflowSchemeView(WorkflowScheme entity) {
        super(entity);
    }
}
