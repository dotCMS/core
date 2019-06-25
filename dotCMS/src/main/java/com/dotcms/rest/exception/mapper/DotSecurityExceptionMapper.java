package com.dotcms.rest.exception.mapper;

import javax.ws.rs.ext.Provider;
import com.dotmarketing.exception.DotSecurityException;

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
