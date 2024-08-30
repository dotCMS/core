package com.dotcms.rest.exception.mapper;

import com.dotcms.exception.ExceptionUtil;
import com.fasterxml.jackson.databind.JsonMappingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.HttpStatusCodeException;
import com.dotcms.rest.exception.ValidationException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;

/**
 * Handles error on the Jackson mapping
 * @author jsanca
 */
@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    private final static Set<Class<? extends Throwable>> EXCEPTIONS =
                    ImmutableSet.of(WebApplicationException.class, ValidationException.class, BadRequestException.class, HttpStatusCodeException.class);

    @Override
    public Response toResponse(final JsonMappingException exception)
    {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        final Optional<Throwable> throwable = ExceptionUtil.getCause(exception,  EXCEPTIONS);
        //Return 4xx message to the client.
        return throwable.isPresent()?
                WebApplicationException.class.cast(throwable.get()).getResponse():
                ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()), exception.getMessage());
    }
}
