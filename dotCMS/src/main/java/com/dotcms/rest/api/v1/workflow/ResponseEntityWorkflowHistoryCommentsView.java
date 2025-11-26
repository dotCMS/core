package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.workflows.model.WorkflowTimelineItem;

import java.util.List;

/**
 * This class is used to return a list of WorkflowTimelineItem objects as a response entity.
 */
public class ResponseEntityWorkflowHistoryCommentsView extends ResponseEntityView<List<WorkflowTimelineItemView>> {

    public ResponseEntityWorkflowHistoryCommentsView(final List<WorkflowTimelineItemView> entity) {
        super(entity);
    }
}
