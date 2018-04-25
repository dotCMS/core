package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception to report when the user is not allowed to update the workflow
 * (aka: does not have access to the workflow portlet)
 * @author jsanca
 */
public class WorkflowPortletAccessException extends DotRuntimeException {

    public WorkflowPortletAccessException(final String message) {
        super(message);
    }

    public WorkflowPortletAccessException(final Throwable throwable) {
        super(throwable);
    }

    public WorkflowPortletAccessException(final String message, final Throwable e) {
        super(message, e);
    }
}
