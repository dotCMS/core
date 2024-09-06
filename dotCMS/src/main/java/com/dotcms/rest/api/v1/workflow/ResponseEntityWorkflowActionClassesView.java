package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;

import java.util.List;

public class ResponseEntityWorkflowActionClassesView extends ResponseEntityView<List<WorkflowActionClass>> {
    public ResponseEntityWorkflowActionClassesView(List<WorkflowActionClass> actionClasses) {
        super(actionClasses);
    }
}
