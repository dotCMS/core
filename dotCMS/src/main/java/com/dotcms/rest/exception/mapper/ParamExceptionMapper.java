package com.dotcms.rest.exception.mapper;

import com.dotcms.exception.ExceptionUtil;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ParamException;
import com.dotmarketing.util.Logger;

/**
 *
 * Instead of returning 404 error, on {@link ParamException} we want to return 400
 */
@Provider
public class ParamExceptionMapper implements ExceptionMapper<ParamException> {

    @Override
    public Response toResponse(final ParamException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        //Create the message.
        final String message = ExceptionUtil.getRootCause(exception).getMessage();

        //Creating the message in JSON format.
        final String entity = ExceptionMapperUtil.getJsonErrorAsString(exception.getParameterName(), message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }
}
