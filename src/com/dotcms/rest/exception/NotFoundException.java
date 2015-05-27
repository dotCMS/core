package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

/**
 * @author Geoff M. Granum
 */
public class NotFoundException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.not_found";

    public NotFoundException(String message, String... messageArgs) {
        super(Response.Status.NOT_FOUND, ERROR_KEY, message, messageArgs);
    }
}
 
