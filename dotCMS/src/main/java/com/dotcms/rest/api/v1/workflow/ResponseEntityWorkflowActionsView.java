package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

public class ResponseEntityWorkflowActionsView extends ResponseEntityView<List<WorkflowActionView>> {
    public ResponseEntityWorkflowActionsView(List<WorkflowActionView> entity) {
        super(entity);
    }
}
