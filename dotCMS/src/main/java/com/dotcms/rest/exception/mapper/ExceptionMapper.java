package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.util.Logger;

/**
 * Generic exception handler to avoid cyclical issues with the stack trace mapping
 *
 * @author jsanca
 */
@Provider
public class ExceptionMapper
    implements com.dotcms.repackage.javax.ws.rs.ext.ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {

    // Log into our logs first.
    Logger.warn(this.getClass(), exception.getMessage(), exception);

    return ExceptionMapperUtil.createResponse(exception, Response.Status.BAD_REQUEST);
  }
}
