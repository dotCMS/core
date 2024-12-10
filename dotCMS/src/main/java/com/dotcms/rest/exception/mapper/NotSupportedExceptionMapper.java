package com.dotcms.rest.exception.mapper;

import com.dotmarketing.util.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotSupportedExceptionMapper implements ExceptionMapper<javax.ws.rs.NotSupportedException> {

    @Override
    public Response toResponse(javax.ws.rs.NotSupportedException exception) {

        Logger.warn(this.getClass(), exception.getMessage(), exception);
        return ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()),
                exception.getMessage(), Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }
}