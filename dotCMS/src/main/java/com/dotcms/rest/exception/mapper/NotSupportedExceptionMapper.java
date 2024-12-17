package com.dotcms.rest.exception.mapper;

import com.dotmarketing.util.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception mapper for handling NotSupportedException.
 * <p>
 * This class maps the javax.ws.rs.NotSupportedException to a proper HTTP response.
 * It logs the warning and returns a response with a status of UNSUPPORTED_MEDIA_TYPE.
 * </p>
 */
@Provider
public class NotSupportedExceptionMapper implements ExceptionMapper<javax.ws.rs.NotSupportedException> {

    /**
     * Converts a NotSupportedException into an HTTP response.
     *
     * @param exception The NotSupportedException that was thrown.
     * @return A Response object containing the error message and a status of UNSUPPORTED_MEDIA_TYPE.
     */
    @Override
    public Response toResponse(javax.ws.rs.NotSupportedException exception) {

        Logger.warn(this.getClass(), exception.getMessage(), exception);
        return ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()),
                exception.getMessage(), Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }
}