package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

/**
 * @author Geoff M. Granum
 */
public class ForbiddenException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.forbidden";

    public ForbiddenException(Throwable cause, String message, String... messageArgs) {
        super(cause, Response.Status.FORBIDDEN, ERROR_KEY, message, messageArgs);
    }
}
 
