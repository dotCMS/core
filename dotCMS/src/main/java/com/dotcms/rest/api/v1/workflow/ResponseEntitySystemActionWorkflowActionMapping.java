package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;

public class ResponseEntitySystemActionWorkflowActionMapping extends ResponseEntityView<SystemActionWorkflowActionMapping> {
    public ResponseEntitySystemActionWorkflowActionMapping(SystemActionWorkflowActionMapping entity) {
        super(entity);
    }
}
