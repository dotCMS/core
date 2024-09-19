package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

public class ResponseEntityDefaultWorkflowActionsView extends ResponseEntityView<List<WorkflowDefaultActionView>> {
    public ResponseEntityDefaultWorkflowActionsView(List<WorkflowDefaultActionView> entity) {
        super(entity);
    }
}
