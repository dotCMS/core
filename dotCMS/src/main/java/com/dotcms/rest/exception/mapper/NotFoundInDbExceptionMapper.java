package com.dotcms.rest.exception.mapper;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.SecurityLogger;

public class NotFoundInDbExceptionMapper
        implements com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper<NotFoundInDbException> {

    private static final String ERROR_KEY = "dotcms.api.error.db.not.found";

    @Override
    public Response toResponse(final NotFoundInDbException exception) {
        SecurityLogger.logInfo(NotFoundInDbExceptionMapper.class, exception.getMessage());

        return ExceptionMapperUtil.createResponse(exception, ERROR_KEY, Response.Status.NOT_FOUND);
    }
}
