package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;

/**
 * It is a base class for {@link ExceptionMapper}
 *
 * @param <T>  <T> Exception class to mapper
 */
public abstract class DotExceptionMapper<T extends Throwable>
        implements javax.ws.rs.ext.ExceptionMapper<T> {

    @Override
    public Response toResponse(final T exception) {
        SecurityLogger.logInfo(DotSecurityExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), Response.Status.FORBIDDEN);
    }

    protected abstract String getErrorKey();
    protected abstract Response.Status getErrorStatus();
}
