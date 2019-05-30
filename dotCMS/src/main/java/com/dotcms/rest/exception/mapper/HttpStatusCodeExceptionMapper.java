package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotcms.rest.exception.HttpStatusCodeException;
import com.dotmarketing.util.Logger;

/**
 * Handles a HttpStatusCodeException using the status on the
 * @author jsanca
 */
@Provider
public class HttpStatusCodeExceptionMapper implements ExceptionMapper<HttpStatusCodeException> {

    @Override
    public Response toResponse(final HttpStatusCodeException exception) {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        return exception.getResponse();

    }
}
