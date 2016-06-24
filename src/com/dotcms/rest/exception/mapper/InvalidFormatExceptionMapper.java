package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
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

    @Override
    public Response toResponse(InvalidFormatException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        //Create the message.
        String message = "Can not construct instance from value '" + exception.getValue()
                        + "': not a valid " + exception.getTargetType() + " value";

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }
}
