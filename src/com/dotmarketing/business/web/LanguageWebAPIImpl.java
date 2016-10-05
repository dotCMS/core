package com.dotmarketing.business.web;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;

/**
 * Implement LanguageWebAPI methods to manage language cache and language struts
 * 
 * @author Oswaldo
 * @author David H Torres
 * @version 1.9
 * 
 */
public class LanguageWebAPIImpl implements LanguageWebAPI {

	private LanguageAPI langAPI;

	public LanguageWebAPIImpl() {
		langAPI = APILocator.getLanguageAPI();
	}

	/**
	 * Clear the language cache and struts cache
	 * 
	 * @throws DotRuntimeException
	 */
	public void clearCache() throws DotRuntimeException {

		langAPI.clearCache();
		MultiMessageResources messages = (MultiMessageResources) MultiMessageResourcesFactory.getResources();
		messages.reload();

	}

	public void checkSessionLocale(HttpServletRequest httpRequest) {

		String languageId = String.valueOf(langAPI.getDefaultLanguage().getId());
		Language currentLang = langAPI.getLanguage(languageId);
		Locale locale = new Locale(currentLang.getLanguageCode(), currentLang.getCountryCode());
		HttpSession sessionOpt = httpRequest.getSession(false);

		boolean validLang = true;
		
		// Priority:
		// 1. request.getParameter(WebKeys.HTMLPAGE_LANGUAGE)
		// 2. request.getParameter("language_id")
		// 4. request.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE)
		// 3. request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)
		//
		// Note: For now we can't ignore request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE) because there
		// are a lot of places still using it. For example the Site Search.
		//
		// Another Note: VTL_INCLUDE -> widgetCode's hidden code sets 1 as request attribute
		// that is the reason why we check the session value first, in order to not break:
		// https://github.com/dotCMS/core/issues/9576
		if ((UtilMethods.isSet(httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE)) ||
			UtilMethods.isSet(httpRequest.getParameter("language_id")) ||
			(sessionOpt != null && UtilMethods.isSet(sessionOpt.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)))) ||
			UtilMethods.isSet(httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))) {

			if (UtilMethods.isSet(httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE))) {
				languageId = httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE);
			} else if(UtilMethods.isSet(httpRequest.getParameter("language_id"))) {
				languageId = httpRequest.getParameter("language_id");
			} else if(sessionOpt!= null && UtilMethods.isSet(sessionOpt.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))) {
				languageId = (String)sessionOpt.getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
			} else if(UtilMethods.isSet(httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))) {
				languageId = httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE).toString();
			}

			//If languageId is not Long we will use the Default Language and log.
			long languageIdLong = APILocator.getLanguageAPI().getDefaultLanguage().getId();
			try{
				languageIdLong = Long.parseLong(languageId);
			} catch (Exception e){
				validLang = false;
				Logger.error(this.getClass(),
						"Language Id from request is not a long value. " +
								"We will use Default Language. " +
								"Value from request: " + languageId, e);
			}

			currentLang = langAPI.getLanguage(languageIdLong);
			locale = new Locale(currentLang.getLanguageCode(), currentLang.getCountryCode());
		}
		
		// if we are changing the language, we NEED a session
		boolean changeLang = false;
		if (validLang &&
			(UtilMethods.isSet(httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE)) ||
				UtilMethods.isSet(httpRequest.getParameter("language_id")))){

			changeLang=true;
		}
		
		if(validLang) {
			httpRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
			httpRequest.setAttribute(WebKeys.LOCALE, locale);
		}

		if(sessionOpt != null || changeLang){
			sessionOpt= httpRequest.getSession(true);
			sessionOpt.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
			boolean ADMIN_MODE = (sessionOpt.getAttribute(WebKeys.ADMIN_MODE_SESSION) != null);

			if (ADMIN_MODE == false || httpRequest.getParameter("leftMenu") == null) {
				sessionOpt.setAttribute(WebKeys.Globals_FRONTEND_LOCALE_KEY, locale);
				httpRequest.setAttribute(WebKeys.Globals_FRONTEND_LOCALE_KEY, locale);
			}

			sessionOpt.setAttribute(WebKeys.LOCALE, locale);
		}
	}
	
	@Override
	public Language getLanguage(HttpServletRequest req) {

		checkSessionLocale(req);
		return APILocator.getLanguageAPI().getLanguage((String) req.getAttribute(WebKeys.HTMLPAGE_LANGUAGE));

	}
	
	
	

}
