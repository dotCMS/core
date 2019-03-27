package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

public class InvalidRuleParameterException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.invalid_parameter";

    public InvalidRuleParameterException(String message, String... messageArgs) {
        super(Response.Status.BAD_REQUEST, ERROR_KEY, message, messageArgs);
    }
}

