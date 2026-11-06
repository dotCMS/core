package com.dotcms.rest.exception.mapper;

import com.dotcms.content.index.SearchUnavailableException;
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


    /** Handles overload without per-request stack traces or query-bearing diagnostic messages. */
    @Override
    public Response toResponse(final DotDataException exception) {
        if (SearchUnavailableException.find(exception) != null) {
            return ExceptionMapperUtil.createSearchUnavailableResponse();
        }
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
