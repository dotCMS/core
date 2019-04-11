package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;

public abstract class DotBadRequestExceptionMapper<T extends Throwable> extends DotExceptionMapper<T> {

    @Override
    public Response toResponse(final T exception) {
        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), this.getErrorStatus());
    }

    protected Response.Status getErrorStatus() {
        return Response.Status.BAD_REQUEST;
    }

    protected String getErrorKey() {
        return "bad-request-exception";
    }
}
