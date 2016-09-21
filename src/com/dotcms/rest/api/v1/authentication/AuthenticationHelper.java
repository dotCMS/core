package com.dotcms.rest.api.v1.authentication;

import com.dotcms.exception.InternationalizationExceptionSupport;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.MessageFormat;
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
    
    /**
	 * Add a info message in the security log
	 * @param dotCMSClass Class responsible for the message
	 * @param message Message to be printed in the log
	 */
	public void securityLog(Class dotCMSClass, String message) {
		SecurityLogger.logInfo(dotCMSClass, message);		
	}
	
	/**
	 * Get the translation of the message key in the specified locale, if the locale is null
	 * then the message is translated into the default user language
	 * @param locale Current user language
	 * @param messageKey Message key to be translated
	 * @param messageArguments (Optional) if the message require some argument
	 * @return
	 */
	public String getFormattedMessage(Locale locale, String messageKey,Object... messageArguments){
		String message;
		try {
			message =(UtilMethods.isSet(locale))? 
					LanguageUtil.get(locale, messageKey):
						LanguageUtil.get((User)null, messageKey);

					return MessageFormat.format(message, messageArguments);
		} catch (LanguageException e) {
			Logger.error(AuthenticationHelper.class, e.getMessage(), e);
			throw new RuntimeException(e);
		}	
	}

} // E:O:F:AuthenticationHelper.
