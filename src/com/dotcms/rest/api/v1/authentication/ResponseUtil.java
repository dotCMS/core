package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Locale;

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

    /**
     * Get the translation of the message key in the specified locale, if the locale is null
     * then the message is translated into the default user language
     *
     * @param locale           Current user language
     * @param messageKey       Message key to be translated
     * @param messageArguments (Optional) if the message require some argument
     * @return
     */
    public String getFormattedMessage(Locale locale, String messageKey, Object... messageArguments) {
        String message;
        try {
            message = (UtilMethods.isSet(locale)) ?
                    LanguageUtil.get(locale, messageKey) :
                    LanguageUtil.get((User) null, messageKey);

            return MessageFormat.format(message, messageArguments);
        } catch (LanguageException e) {
            Logger.error(AuthenticationHelper.class, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

} // E:O:F:ResponseUtil.
