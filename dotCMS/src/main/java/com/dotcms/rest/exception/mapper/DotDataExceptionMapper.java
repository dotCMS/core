package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

/**
 * End point Mapping exception for {@link com.dotmarketing.exception.DotSecurityException}
 */
@Provider
public class DotDataExceptionMapper extends DotBadRequestExceptionMapper<DotDataException> {


    @Override
    public Response toResponse(final DotDataException exception) {
        Logger.error(this, exception.getMessage(), exception);
        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), this.getErrorStatus());
    }

    protected Response.Status getErrorStatus() {
        return Response.Status.BAD_REQUEST;
    }

    protected String getErrorKey() {
        return "bad-request-exception";
    }
}
