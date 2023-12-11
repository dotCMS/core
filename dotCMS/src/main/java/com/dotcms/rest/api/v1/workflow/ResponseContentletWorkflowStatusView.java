package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;

/**
 * This custom response object provides the Entity View that represents the current status of a
 * Contentlet inside a Workflow.
 *
 * @author Jose Castro
 * @since Dec 7th, 2023
 */
public class ResponseContentletWorkflowStatusView extends ResponseEntityView<ContentletWorkflowStatusView> {

    public ResponseContentletWorkflowStatusView(final ContentletWorkflowStatusView entity) {
        super(entity);
    }

}
