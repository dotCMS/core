package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;

/**
 * It is a base class for exception mapper that need to response with a HTTP code of 403
 *
 * @param <T>  <T> Exception class to mapper
 */
public abstract class DotForbiddenExceptionMapper<T extends Throwable> extends DotExceptionMapper<T> {

    @Override
    public Response toResponse(final T exception) {
        SecurityLogger.logInfo(this.getClass(), exception.getMessage());
        Logger.warnAndDebug(this.getClass(), exception);

        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), this.getErrorStatus());
    }

    protected Response.Status getErrorStatus() {
        return Response.Status.FORBIDDEN;
    }

    protected abstract String getErrorKey();
}
