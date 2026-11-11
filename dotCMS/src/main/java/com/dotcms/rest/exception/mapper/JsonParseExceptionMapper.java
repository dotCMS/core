package com.dotcms.rest.exception.mapper;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;

/**
 * Created by Oscar Arrieta on 8/27/15.
 *
 * Instead of returning 5xx error, we need a way to tell the user is their fault that the Rest call is not working.
 * With this class we catch JsonParseException and print our own status error and message in order to
 * alert the user they are sending incorrect data and should fix it before try again.
 */
@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    /**
     * Answers a request whose body is not valid JSON. The log line gives the position of the
     * problem only; the body text is left out.
     *
     * @param exception the parse failure raised by Jackson
     * @return a 400 response describing the problem
     */
    @Override
    public Response toResponse(final JsonParseException exception)
    {
        //Log into our logs first.
        final JsonLocation location = exception.getLocation();
        Logger.warn(this.getClass(), "Rejected request: the body is not valid JSON"
                + (location != null
                        ? " (line " + location.getLineNr() + ", column " + location.getColumnNr() + ")"
                        : ""));

        //Create the message.
        String message = exception.getMessage();

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }
}
