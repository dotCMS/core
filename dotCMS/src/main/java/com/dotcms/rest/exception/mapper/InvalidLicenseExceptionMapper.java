package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.Logger;

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
