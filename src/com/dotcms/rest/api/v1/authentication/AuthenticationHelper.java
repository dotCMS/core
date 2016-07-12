package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.entry;
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

        try {

            return Response.status(status).entity
                (new ResponseEntityView
                    (Arrays.asList(new ErrorEntity(messageKey,
                            LanguageUtil.get(locale,
                                    messageKey))))).build();


        } catch (LanguageException e1) {
            // Quiet
        }

        return null;
    }

} // E:O:F:AuthenticationHelper.
