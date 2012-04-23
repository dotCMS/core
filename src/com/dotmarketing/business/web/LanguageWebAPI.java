package com.dotmarketing.business.web;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * Specialized LanguageAPI created to manage language cache and language struts cache in webdav.  
 * @author Oswaldo
 * @version 1
 */
public interface LanguageWebAPI {

	/**
	 * Clear the language cache and struts cache
	 * @throws DotRuntimeException
	 */
	public abstract void clearCache() throws DotRuntimeException;
	
	/**
	 * Checks and correctly sets in session the user current selected language and locale setting
	 * After invoking this method the following objects get set in session and request object with the correct values:
	 * 
	 * com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE - With the language id
	 * com.dotmarketing.util.WebKeys.LOCALE - With the user locale 
	 * 
	 * @param httpRequest
	 */
	void checkSessionLocale(HttpServletRequest httpRequest);

	
	/**
	 * Checks the session and gets the language for the user
	 * @param req
	 * @return
	 */
	Language getLanguage(HttpServletRequest req);
	
	

}
