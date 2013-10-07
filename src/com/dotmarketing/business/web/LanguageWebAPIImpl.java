package com.dotmarketing.business.web;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
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
		HttpSession session = httpRequest.getSession();

		// set default page language
		if (UtilMethods.isSet((String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE))) {

			languageId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
			currentLang = langAPI.getLanguage(languageId);
			locale = new Locale(currentLang.getLanguageCode(), currentLang.getCountryCode());

		}

		// update page language
		if (UtilMethods.isSet(httpRequest.getParameter(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE))
				|| UtilMethods.isSet(httpRequest.getParameter("language_id"))
				|| UtilMethods.isSet(httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))) {
			if (UtilMethods.isSet(httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE))) {
				languageId = httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE);
			} else if(UtilMethods.isSet(httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))) {
			    languageId = (String)httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
			}
			else {
				languageId = httpRequest.getParameter("language_id");
			}
			currentLang = langAPI.getLanguage(languageId);
			locale = new Locale(currentLang.getLanguageCode(), currentLang.getCountryCode());

		}

		session.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
		httpRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
		boolean ADMIN_MODE = (session.getAttribute(WebKeys.ADMIN_MODE_SESSION) != null);
		if (ADMIN_MODE == false || httpRequest.getParameter("leftMenu") == null) {
			session.setAttribute(WebKeys.Globals_FRONTEND_LOCALE_KEY, locale);
			httpRequest.setAttribute(WebKeys.Globals_FRONTEND_LOCALE_KEY, locale);
		}
		session.setAttribute(WebKeys.LOCALE, locale);
		httpRequest.setAttribute(WebKeys.LOCALE, locale);

	}
	
	@Override
	public Language getLanguage(HttpServletRequest req) {

		checkSessionLocale(req);
		return APILocator.getLanguageAPI().getLanguage((String) req.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE));

	}
	
	
	

}
