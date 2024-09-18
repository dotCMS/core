package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import java.util.List;

public class ResponseEntitySystemActionWorkflowActionMappings extends ResponseEntityView<List<SystemActionWorkflowActionMapping>> {
    public ResponseEntitySystemActionWorkflowActionMappings(List<SystemActionWorkflowActionMapping> entity) {
        super(entity);
    }
}
