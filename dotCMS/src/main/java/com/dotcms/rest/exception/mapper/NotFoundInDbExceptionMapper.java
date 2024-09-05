package com.dotcms.rest.exception.mapper;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;
import javax.ws.rs.ext.Provider;

/**
 * Mapper for {@link NotFoundInDbException}
 */
@Provider
public class NotFoundInDbExceptionMapper
        implements javax.ws.rs.ext.ExceptionMapper<NotFoundInDbException> {

    private static final String ERROR_KEY = "dotcms.api.error.db.not.found";

    @Override
    public Response toResponse(final NotFoundInDbException exception) {
        SecurityLogger.logInfo(NotFoundInDbExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, ERROR_KEY, Response.Status.NOT_FOUND);
    }
}
