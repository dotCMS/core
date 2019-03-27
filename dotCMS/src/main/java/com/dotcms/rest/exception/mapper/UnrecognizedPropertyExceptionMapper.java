package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;


@Provider
public class UnrecognizedPropertyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {

    @Override
    public Response toResponse(UnrecognizedPropertyException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        //Create the message.
        String message = exception.getMessage();

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }
}
