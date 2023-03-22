package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.workflow.WorkflowActionView;

import java.util.List;

/**
 * Encapsulates a list of {@link WorkflowActionView}
 * @author jsanca
 */
public class ResponseEntityWorkflowActionsView extends ResponseEntityView<List<WorkflowActionView>> {
    public ResponseEntityWorkflowActionsView(final List<WorkflowActionView> entity) {
        super(entity);
    }
}
