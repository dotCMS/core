package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorResponseHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Locale;

import static com.dotcms.util.CollectionsUtils.mapEntries;

/**
 * Just a helper to encapsulate AuthenticationResource functionality.
 * @author jsanca
 */
public class AuthenticationHelper implements Serializable {

    public static AuthenticationHelper INSTANCE =
            new AuthenticationHelper();


    private AuthenticationHelper () {

    }

    /**
     * Get Error response based on a status and message key
     * This support is a single message
     *
     * @param request
     * @param status
     * @param userId
     * @param messageKey
     * @return Response
     */
    public Response getErrorResponse(final HttpServletRequest request,
                                     final Response.Status status,
                                     final Locale locale,
                                     final String userId,
                                     final String messageKey) {

        return ErrorResponseHelper.INSTANCE.getErrorResponse(status, locale, messageKey);
    }

} // E:O:F:AuthenticationHelper.
