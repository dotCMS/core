package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * Handles a HttpStatusCodeException using the status on the
 * @author jsanca
 */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(final ResourceNotFoundException exception) {
        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        //Return 4xx message to the client.
        return
                ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()), exception.getMessage(), Response.Status.NOT_FOUND);

    }
}
