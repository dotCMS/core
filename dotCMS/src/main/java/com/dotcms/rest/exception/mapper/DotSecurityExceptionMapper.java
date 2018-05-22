package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.Logger;

/**
* End point Mapping exception for {@link com.dotmarketing.exception.DotSecurityException}
 */
@Provider
public class DotSecurityExceptionMapper implements com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper<DotSecurityException>{

    private static final String ERROR_KEY = "dotcms.api.error.forbidden";

    @Override
    public Response toResponse(final DotSecurityException exception) {
        final String errorMsg = "The user does not have the required permissions (" + exception
                .getMessage() + ")";
        Logger.error(this, errorMsg, exception);

        return ExceptionMapperUtil.createResponse(exception, ERROR_KEY, Response.Status.FORBIDDEN);
    }
}
