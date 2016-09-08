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
public class ResponseUtil implements Serializable {

    public static ResponseUtil INSTANCE =
            new ResponseUtil();


    private ResponseUtil() {

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

} // E:O:F:ResponseUtil.
