package com.dotcms.rest.exception;

import javax.ws.rs.core.Response;

/**
 *
 */
public class ServiceUnavailableException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.service_unavailable";

    public ServiceUnavailableException(final String message) {
        super(Response.Status.SERVICE_UNAVAILABLE, ERROR_KEY, message);
    }

    public ServiceUnavailableException(final Throwable cause, final String message, final String... messageArgs) {
        super(cause, Response.Status.BAD_REQUEST, ERROR_KEY, message, messageArgs);
    }
}
