package com.dotcms.rest.exception.mapper;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.HttpStatusCodeException;
import com.dotcms.rest.exception.ValidationException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Handles error on the Jackson mapping
 * @author jsanca
 */
@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<ValueInstantiationException> {

    private static final Set<Class<? extends Throwable>> EXCEPTIONS =
                    Set.of(WebApplicationException.class, ValidationException.class, BadRequestException.class, HttpStatusCodeException.class);

    /**
     * Answers a request whose body could not be turned into the resource's form object. The log
     * line names the target type only; the body text is left out.
     *
     * @param exception the instantiation failure raised by Jackson
     * @return the response carried by a known cause, or a 400 response describing the problem
     */
    @Override
    public Response toResponse(final ValueInstantiationException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), "Rejected request: the body could not be converted to "
                + (exception.getType() != null
                        ? exception.getType().getRawClass().getSimpleName()
                        : "the expected type"));

        final Optional<Throwable> throwable = ExceptionUtil.getCause(exception,  EXCEPTIONS);
        //Return 4xx message to the client.
        return throwable.isPresent()?
                ((WebApplicationException) throwable.get()).getResponse():
                ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()), exception.getMessage());
    }
}
