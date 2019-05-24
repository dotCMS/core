package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * Handles a ResourceNotFoundException by returning 404
 * @author jsanca
 */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(final ResourceNotFoundException exception) {
        
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        return
                ExceptionMapperUtil.createResponse(ExceptionMapperUtil.getJsonErrorAsString(exception.getMessage()), exception.getMessage(), Response.Status.NOT_FOUND);

    }
}
