package com.dotcms.rest.exception.mapper;

import javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.InvalidLicenseException;

/**
 * End point Mapping exception for {@link com.dotmarketing.exception.InvalidLicenseException}
 */
@Provider
public class InvalidLicenseExceptionMapper extends DotForbiddenExceptionMapper<InvalidLicenseException> {

    private static final String ERROR_KEY = "dotcms.api.error.license.required";

    @Override
    protected String getErrorKey() {
        return ERROR_KEY;
    }
}
