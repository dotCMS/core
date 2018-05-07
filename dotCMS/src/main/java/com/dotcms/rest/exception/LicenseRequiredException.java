package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.core.Response;

/**
 * Throw when feature is EE only
 */
public class LicenseRequiredException extends HttpStatusCodeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_KEY = "dotcms.api.error.license.required";

    public LicenseRequiredException() {
        this("Need an enterprise license to run this functionality.");
    }

    public LicenseRequiredException(String message) {
        super(Response.Status.FORBIDDEN, ERROR_KEY, message);
    }
}
