package com.dotcms.rest.exception;

import javax.ws.rs.core.Response;

/**
 * @author Geoff M. Granum
 */
public class ForbiddenException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.forbidden";

    public ForbiddenException(final String message, final String... messageArgs) {
        super(Response.Status.FORBIDDEN, ERROR_KEY, message, messageArgs);
    }

    public ForbiddenException(final Throwable cause, final String message, final String... messageArgs) {
        super(cause, Response.Status.FORBIDDEN, ERROR_KEY, message, messageArgs);
    }

    public ForbiddenException(final Throwable cause) {
        super(cause, Response.Status.FORBIDDEN, ERROR_KEY, cause.getMessage());
    }
}
 
