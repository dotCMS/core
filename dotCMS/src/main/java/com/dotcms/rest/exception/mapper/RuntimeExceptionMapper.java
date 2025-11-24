package com.dotcms.rest.exception.mapper;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.util.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Generic runtime exception handler to avoid cyclical issues with the stack trace mapping
 * @author jsanca
 */
@Provider
public class RuntimeExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {

        //Log into our logs first.
        if(ExceptionUtil.causedBy(exception, ExceptionUtil.LOUD_MOUTH_EXCEPTIONS)){
            Logger.warn(this.getClass(), exception.getMessage());
        } else {
            Logger.warn(this.getClass(), exception.getMessage(), exception);
        }

        return ResponseUtil.mapExceptionResponse(exception);
    }
}
