package com.dotcms.rest.exception;

import javax.ws.rs.core.Response;

/**
 * Exception class representing a generic HTTP status code error.
 *
 * <p>This exception is thrown when a generic HTTP status code error occurs in the application.
 * It extends the {@link HttpStatusCodeException} class and provides additional constructors
 * to handle different error scenarios.</p>
 *
 * @author vico
 */
public class GenericHttpStatusCodeException extends HttpStatusCodeException {

    public GenericHttpStatusCodeException(final String message, final Response.Status status) {
        super(null, status, null, message);
    }
}
 
