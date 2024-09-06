package com.dotcms.rest.exception.mapper;

import com.dotcms.exception.NotAllowedException;
import com.dotmarketing.util.SecurityLogger;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

/**
 * Mapper for {@link NotAllowedException}
 */
@Provider
public class NotAllowedExceptionMapper
        implements javax.ws.rs.ext.ExceptionMapper<NotAllowedException> {

    private static final String ERROR_KEY = "dotcms.api.error.not.allowed";

    @Override
    public Response toResponse(final NotAllowedException exception) {
        SecurityLogger.logInfo(NotAllowedExceptionMapper.class, exception.getMessage());
        return ExceptionMapperUtil.createResponse(exception, ERROR_KEY, Status.METHOD_NOT_ALLOWED);
    }
}
