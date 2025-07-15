package com.dotcms.rest;

import javax.ws.rs.core.Response;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

/**
 * Generic error helper for rest stuff
 * @author jsanca
 */
public class ErrorResponseHelper implements Serializable {

    public final static ErrorResponseHelper INSTANCE = new ErrorResponseHelper();

    private ErrorResponseHelper() {}

    /**
     * Get Error response based on a status and message key
     * This support is a single message
     *
     * @param status
     * @param messageKey
     * @return Response
     */
    public Response getErrorResponse(final Response.Status status,
                                     final Locale locale,
                                     final String messageKey,
                                     final Object... arguments) {

        try {

            return Response.status(status).entity
                    (new ResponseEntityView<>
                            (Arrays.asList(new ErrorEntity(messageKey,
                                    LanguageUtil.get(locale,
                                            messageKey, arguments))))).build();


        } catch (LanguageException e1) {
            // Quiet
        }

        return null;
    }
} // E:O:F:ErrorResponseHelper.
