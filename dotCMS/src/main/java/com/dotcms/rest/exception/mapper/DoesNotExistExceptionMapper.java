package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.SecurityLogger;

/**
 * Mapper for {@link DoesNotExistException}
 */
public class DoesNotExistExceptionMapper
        implements com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper<DoesNotExistException> {

    private static final String ERROR_KEY = "dotcms.api.error.db.not.found";

    @Override
    public Response toResponse(final DoesNotExistException exception) {
        SecurityLogger.logInfo(DoesNotExistExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, ERROR_KEY, Response.Status.NOT_FOUND);
    }
}
