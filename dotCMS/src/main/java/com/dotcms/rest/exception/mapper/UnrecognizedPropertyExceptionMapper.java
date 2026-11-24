package com.dotcms.rest.exception.mapper;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;


@Provider
public class UnrecognizedPropertyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {

    @Override
    public Response toResponse(UnrecognizedPropertyException exception)
    {
        //Log into our logs first, without the field name the client sent.
        Logger.warn(this.getClass(), "Rejected request: the body has a field that "
                + (exception.getReferringClass() != null
                        ? exception.getReferringClass().getSimpleName()
                        : "the target form")
                + " does not declare");

        //Create the message.
        String message = exception.getMessage();

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }
}
