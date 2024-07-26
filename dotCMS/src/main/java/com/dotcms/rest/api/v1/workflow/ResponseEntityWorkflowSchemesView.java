package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

import java.util.List;

public class ResponseEntityWorkflowSchemesView extends ResponseEntityView<List<WorkflowScheme>> {
    public ResponseEntityWorkflowSchemesView(List<WorkflowScheme> schemes) {
        super(schemes);
    }
}
