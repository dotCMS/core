package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

public class InvalidConditionParameterException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.invalid_parameter";

    public InvalidConditionParameterException(String message, String... messageArgs) {
        super(Response.Status.NOT_FOUND, ERROR_KEY, message, messageArgs);
    }
}

