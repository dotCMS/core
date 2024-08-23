package com.dotcms.rest.exception.mapper;

import com.dotmarketing.util.SecurityLogger;
import javax.ws.rs.core.Response;

/**
 * Mapper for all the exceptions that represent a BadRequest
 *
 * @see com.dotmarketing.business.DotStateException
 * @see com.dotmarketing.exception.AlreadyExistException
 * @see IllegalArgumentException
 * @see com.dotmarketing.exception.DotDataValidationException
 * @see com.fasterxml.jackson.core.JsonProcessingException
 *
 * @param <T> Exception class to mapper
 */
public abstract class DotBadRequestExceptionMapper<T extends Throwable> extends DotExceptionMapper<T> {

    @Override
    public Response toResponse(final T exception) {
        SecurityLogger.logInfo(DotBadRequestExceptionMapper.class, exception.getMessage());
        return ExceptionMapperUtil.createResponse(exception, this.getErrorKey(), this.getErrorStatus());
    }

    protected Response.Status getErrorStatus() {
        return Response.Status.BAD_REQUEST;
    }

    protected String getErrorKey() {
        return "bad-request-exception";
    }
}
