package com.dotcms.exception;

import java.text.MessageFormat;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

/**
 * Define the internationalization exception methods to be implemented 
 * in the exception classes where this interface is implemented
 * @author oswaldogallango
 *
 */
public interface InternationalizationExceptionSupport {

	/**
	 * Format the messagekey into the current language provided on the current locale request 
	 * or into the default user locale
	 * @return a string with the messagekey translated into the correct language
	 */
	default String getFormattedMessage(){
		String message;
		try {

			HttpServletRequest req = HttpServletRequestThreadLocal.INSTANCE.getRequest();
			Locale locale = LocaleUtil.getLocale(req);
			message =(UtilMethods.isSet(locale))? 
					LanguageUtil.get(locale, this.getMessageKey()):
						LanguageUtil.get((User)null, this.getMessageKey());

					return MessageFormat.format(message, this.getMessageArguments());
		} catch (LanguageException e) {
			Logger.error(InternationalizationExceptionSupport.class, e.getMessage(), e);
			throw new RuntimeException(e);
		}	
	}
	
	/**
	 * Format the messagekey into the specified locale parameter 
	 * or into the default user locale
	 * @return a string with the messagekey translated into the specified language
	 */
	default String getFormattedMessage(Locale locale){
		String message;
		try {

			message =(UtilMethods.isSet(locale))? 
					LanguageUtil.get(locale, this.getMessageKey()):
						LanguageUtil.get((User)null, this.getMessageKey());

					return MessageFormat.format(message, this.getMessageArguments());
		} catch (LanguageException e) {
			Logger.error(InternationalizationExceptionSupport.class, e.getMessage(), e);
			throw new RuntimeException(e);
		}	
	}
	String getMessageKey();
	Object[] getMessageArguments();


}
