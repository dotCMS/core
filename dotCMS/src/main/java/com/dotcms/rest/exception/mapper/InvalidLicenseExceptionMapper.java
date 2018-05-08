package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.Logger;

/**
 * End point Mapping exception for {@link com.dotmarketing.exception.InvalidLicenseException}
 */
@Provider
public class InvalidLicenseExceptionMapper implements com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper<InvalidLicenseException>{

    private static final String ERROR_KEY = "dotcms.api.error.license.required";

    @Override
    public Response toResponse(InvalidLicenseException exception) {
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        return ExceptionMapperUtil.createResponse(exception, ERROR_KEY, Response.Status.FORBIDDEN);
    }
}
