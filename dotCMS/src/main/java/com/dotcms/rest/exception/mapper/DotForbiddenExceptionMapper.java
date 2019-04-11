package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;

public abstract class DotForbiddenExceptionMapper<T extends Throwable> extends DotExceptionMapper<T> {

    @Override
    public Response toResponse(final T exception) {
        SecurityLogger.logInfo(DotSecurityExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), this.getErrorStatus());
    }

    protected Response.Status getErrorStatus() {
        return Response.Status.FORBIDDEN;
    }

    protected abstract String getErrorKey();
}
