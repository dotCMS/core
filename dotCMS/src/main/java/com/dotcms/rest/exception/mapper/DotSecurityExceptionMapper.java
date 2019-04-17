package com.dotcms.rest.exception.mapper;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;

/**
* End point Mapping exception for {@link com.dotmarketing.exception.DotSecurityException}
 */
@Provider
public class DotSecurityExceptionMapper extends DotForbiddenExceptionMapper<DotSecurityException> {

    private static final String ERROR_KEY = "dotcms.api.error.forbidden";

    @Override
    protected String getErrorKey() {
        return ERROR_KEY;
    }
}
