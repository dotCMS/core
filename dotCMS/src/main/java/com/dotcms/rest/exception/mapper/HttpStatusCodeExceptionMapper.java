package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
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
