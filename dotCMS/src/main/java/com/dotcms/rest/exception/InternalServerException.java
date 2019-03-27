package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

/**
 * @author Geoff M. Granum
 */
public class InternalServerException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.internal_server_error";

    public InternalServerException(String message) {
        super(Response.Status.BAD_REQUEST, ERROR_KEY, message);
    }

    public InternalServerException(Throwable cause, String message, String... messageArgs) {
        super(cause, Response.Status.BAD_REQUEST, ERROR_KEY, message, messageArgs);
    }
}
 
