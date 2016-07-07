package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Just a helper to encapsulate AuthenticationResource functionality.
 * @author jsanca
 */
public class AuthenticationHelper {

    public static AuthenticationHelper INSTANCE =
            new AuthenticationHelper();

    private AuthenticationHelper () {}

    public Response getErrorResponse(final HttpServletRequest request,
                                     final Response.Status status,
                                     final String userId,
                                     final String messageKey) {

        try {

            SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as " + userId.toLowerCase() + " has been made from IP: " + request.getRemoteAddr());

            return Response.status(status).entity
                (new ResponseEntityView
                    (Arrays.asList(new ErrorEntity(messageKey,
                            LanguageUtil.get(request.getLocale(),
                                    messageKey))))).build();


        } catch (LanguageException e1) {
            // Quiet
        }

        return null;
    }
}
