package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;

import java.util.List;

public class ResponseEntityWorkflowActionletsView extends ResponseEntityView<List<WorkFlowActionlet>> {
    public ResponseEntityWorkflowActionletsView(List<WorkFlowActionlet> actionlets) {
        super(actionlets);
    }
}
