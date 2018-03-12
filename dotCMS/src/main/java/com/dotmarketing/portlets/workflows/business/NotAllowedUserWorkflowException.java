package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception to report when the user is not allowed to update the workflow
 * (aka: does not have license or has not access to the workflow portlet)
 * @author jsanca
 */
public class NotAllowedUserWorkflowException extends DotRuntimeException {
    public NotAllowedUserWorkflowException(String x) {
        super(x);
    }

    public NotAllowedUserWorkflowException(Throwable x) {
        super(x);
    }

    public NotAllowedUserWorkflowException(String x, Throwable e) {
        super(x, e);
    }
}
