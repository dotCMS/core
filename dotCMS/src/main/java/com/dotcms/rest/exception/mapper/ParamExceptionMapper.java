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

    /**
     * Answers a request whose parameter could not be converted to the type the resource declares.
     * The log line names the parameter only; the value the client sent is left out.
     *
     * @param exception the conversion failure raised by Jersey
     * @return a 400 response naming the parameter
     */
    @Override
    public Response toResponse(final ParamException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), "Rejected request: " + parameterKind(exception) + " '"
                + exception.getParameterName() + "' could not be converted to its declared type");

        //Create the message.
        final String message = ExceptionUtil.getRootCause(exception).getMessage();

        //Creating the message in JSON format.
        final String entity = ExceptionMapperUtil.getJsonErrorAsString(exception.getParameterName(), message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message);
    }

    /**
     * Returns the kind of parameter that failed, such as {@code QueryParam} or {@code PathParam}.
     */
    private static String parameterKind(final ParamException exception) {
        return exception.getParameterType() != null
                ? exception.getParameterType().getSimpleName()
                : "parameter";
    }
}
