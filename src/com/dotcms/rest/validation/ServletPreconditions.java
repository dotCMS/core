package com.dotcms.rest.validation;

import com.dotmarketing.util.Config;
import javax.servlet.http.HttpServletRequest;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;
import static com.dotcms.rest.validation.Preconditions.newException;

/**
 * @author Geoff M. Granum
 */
public class ServletPreconditions {

    public static <T extends HttpServletRequest> T checkSslIsEnabledIfRequired(T request) {
        checkNotNull(request, com.dotcms.rest.exception.SecurityException.class, "SSL Required");
        if(Config.getBooleanProperty("FORCE_SSL_ON_RESP_API", false) && !request.isSecure()) {
            throw newException("SSL Required", com.dotcms.rest.exception.SecurityException.class);
        }
        return request;
    }
}
 
