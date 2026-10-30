package com.dotcms.rest.exception.mapper;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;

/**
 * Created by Oscar Arrieta on 8/25/15.
 *
 * Instead of returning 5xx error, we need a way to tell the user is their fault that the Rest call is not working.
 * With this class we catch InvalidFormatExceptions and print our own status error and message in order to
 * alert the user they are sending incorrect data and should fix it before try again.
 */
@Provider
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

    /**
     * Answers a request whose body holds a value that cannot be converted to the field's type.
     * The log line names the target type only; the value the client sent is left out.
     *
     * @param exception the conversion failure raised by Jackson
     * @return a 400 response describing the value and the expected type
     */
    @Override
    public Response toResponse(final InvalidFormatException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), "Rejected request: a body value could not be converted to "
                + (exception.getTargetType() != null
                        ? exception.getTargetType().getSimpleName()
                        : "the expected type"));

        //Create the message.
        String message = "Can not construct instance from value '" + exception.getValue()
                        + "': not a valid " + exception.getTargetType() + " value";

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }
}
