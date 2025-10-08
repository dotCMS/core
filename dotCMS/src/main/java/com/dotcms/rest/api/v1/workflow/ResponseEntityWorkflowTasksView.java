package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response Entity to return a collection of Workflow Tasks
 * @author jsanca
 */
public class ResponseEntityWorkflowTasksView extends ResponseEntityView<WorkflowSearchResultsView> {
    public ResponseEntityWorkflowTasksView(final WorkflowSearchResultsView entity) {
        super(entity);
    }
}
