package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

/**
 * @author Geoff M. Granum
 */
public class BadRequestException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.bad_request";

    public BadRequestException(String message) {
        this(null, message, null);
    }

    public BadRequestException(String message, String... messageArgs) {
        this(null, message, messageArgs);
    }

    public BadRequestException(Throwable cause, String message, String... messageArgs) {
        super(cause, Response.Status.BAD_REQUEST, ERROR_KEY, message, messageArgs);
    }
}
 
