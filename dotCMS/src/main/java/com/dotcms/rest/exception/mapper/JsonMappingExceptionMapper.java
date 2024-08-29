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

    @Override
    public Response toResponse(final ValueInstantiationException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        final Optional<Throwable> throwable = ExceptionUtil.getCause(exception,  EXCEPTIONS);
        //Return 4xx message to the client.
        return throwable.isPresent()?
                ((WebApplicationException) throwable.get()).getResponse():
                ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()), exception.getMessage());
    }
}
