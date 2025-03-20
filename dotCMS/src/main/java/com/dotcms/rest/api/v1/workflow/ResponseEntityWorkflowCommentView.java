package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;

/**
 * This class is used to return a WorkflowTimelineItemView object as a response entity.
 * @author jsanca
 */
public class ResponseEntityWorkflowCommentView extends ResponseEntityView<WorkflowTimelineItemView> {
    public ResponseEntityWorkflowCommentView(final WorkflowTimelineItemView entity) {
        super(entity);
    }
}
