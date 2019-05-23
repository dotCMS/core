package com.dotcms.rest.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;

/**
* End point Mapping exception for {@link com.dotmarketing.exception.DotSecurityException}
 */
/*@Provider
<<<<<<< HEAD
public class DotSecurityExceptionMapper extends DotForbiddenExcep\tionMapper<DotSecurityException> {
=======*/
public class DotSecurityExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<DotSecurityException>{
//>>>>>>> origin/master

    private static final String ERROR_KEY = "dotcms.api.error.forbidden";

    @Override
    protected String getErrorKey() {
        return ERROR_KEY;
    }
}
