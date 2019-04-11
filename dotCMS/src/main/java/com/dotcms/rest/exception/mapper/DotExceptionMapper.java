package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;

public abstract class DotExceptionMapper<T extends Throwable>
        implements com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper<T> {

    @Override
    public Response toResponse(final T exception) {
        SecurityLogger.logInfo(DotSecurityExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), Response.Status.FORBIDDEN);
    }

    protected abstract String getErrorKey();
    protected abstract Response.Status getErrorStatus();
}
