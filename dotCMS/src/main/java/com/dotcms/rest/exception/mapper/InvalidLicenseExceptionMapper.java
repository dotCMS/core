package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.Logger;

/**
 * End point Mapping exception for {@link com.dotmarketing.exception.InvalidLicenseException}
 */
@Provider
//<<<<<<< HEAD
//public class InvalidLicenseExceptionMapper extends DotForbiddenExceptionMapper<InvalidLicenseException> {
//=======
public class InvalidLicenseExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<InvalidLicenseException>{
//>>>>>>> origin/master

    private static final String ERROR_KEY = "dotcms.api.error.license.required";

    @Override
    protected String getErrorKey() {
        return ERROR_KEY;
    }
}
