package com.dotcms.rest.exception.mapper;

import com.dotcms.content.index.SearchUnavailableException;
import javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;

/**
 * It is a base class for {@link ExceptionMapper}
 *
 * @param <T>  <T> Exception class to mapper
 */
public abstract class DotExceptionMapper<T extends Throwable>
        implements javax.ws.rs.ext.ExceptionMapper<T> {

    protected DotExceptionMapper() {}

    /** Handles overload without per-request stack traces or query-bearing diagnostic messages. */
    @Override
    public Response toResponse(final T exception) {
        if (SearchUnavailableException.find(exception) != null) {
            return ExceptionMapperUtil.createSearchUnavailableResponse();
        }
        SecurityLogger.logInfo(DotSecurityExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(),  this.getErrorStatus());
    }

    protected abstract String getErrorKey();
    protected abstract Response.Status getErrorStatus();
}
