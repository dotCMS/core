package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowPortletAccessException;
import com.dotmarketing.util.SecurityLogger;

/**
* End point Mapping exception for {@link WorkflowPortletAccessException}
 */
@Provider
public class WorkflowPortletAccessExceptionMapper extends DotForbiddenExceptionMapper<WorkflowPortletAccessException> {

    private static final String ERROR_KEY = "dotcms.api.error.workflow.access";


    @Override
    protected String getErrorKey() {
        return ERROR_KEY;
    }
}
