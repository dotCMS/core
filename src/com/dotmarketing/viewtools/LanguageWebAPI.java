package com.dotmarketing.viewtools;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;


public class LanguageWebAPI implements ViewTool {

	private HttpServletRequest request;
	Context ctx;
	private static LanguageAPI langAPI = APILocator.getLanguageAPI();

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}

	/**
	 * Get Language by language code and country code
	 * @param languageCode
	 * @param countryCode
	 * @return Language
	 */
	public static Language getLanguage(String languageCode, String countryCode) {
		return langAPI.getLanguage(languageCode, countryCode);
	}

	/**
	 * Get Language by language code
	 * @param langId
	 * @return Language
	 */
	public static Language getLanguage(String langId) {
		return langAPI.getLanguage(langId);
	}

	/**
	 * Get the default language
	 * @return Language
	 */
	public static Language getDefaultLanguage() {
		return langAPI.getDefaultLanguage();
	}

	/**
	 * Get the list of availables languages
	 * @return List<Language>
	 */
	public static List<Language> getLanguages() {
		return langAPI.getLanguages();
	}

	/**
	 * Return if the DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE property is activated or not
	 * defaults to false
	 * @return boolean
	 */
	public static boolean canDefaultContentToDefaultLanguage() {
		return Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",false);
	}
	
	/**
	 * Return if the DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE property is activated or not
	 * deaults to true
	 * @return
	 */
	public static boolean canDefaultWidgetToDefaultLanguage() {
        return Config.getBooleanProperty("DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE",true);
    }
	
	/**
	 * Return if the content can be use as a default to all languages.
	 * It is a conjuntion of canDefaultContentToDefaultLanguage(),
	 * wherever it is a form widget and canDefaultWidgetToDefaultLanguage()
	 * if the content is a widget.
	 * This method is intended to keep this rules in one place so it can
	 * be used by DotResourceLoader and content service cache invalidator.
	 * Also the contentlet must live in the default language.
	 * @param cc
	 * @return
	 */
	public static boolean canApplyToAllLanguages(Contentlet cc) {
	    return cc.getLanguageId()==APILocator.getLanguageAPI().getDefaultLanguage().getId() &&
	            (canDefaultContentToDefaultLanguage() // content default to default language?
                || cc.getStructure().getVelocityVarName().equalsIgnoreCase("forms") // forms? apply to all languages
                || (cc.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_WIDGET // widgets default to all langs?
                     && canDefaultWidgetToDefaultLanguage()));
	}

	/**
	 * Update fronend language
	 * @param langId
	 */
	public void setLanguage(String langId) {
		request.setAttribute(WebKeys.LANGUAGE, langId);
	}

	/**
	 * Glosssary webapi
	 * @param langId
	 */
	public String get(String key) {
		String language = null;
		language = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
		if (language == null)
			language = String.valueOf(langAPI.getDefaultLanguage().getId());
		return get(key, language);
	}

	public String get(String key, List args) {
		String language = null;
		if (language == null)
			language = (String) request.getSession().getAttribute(
					com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);

		if (language == null)
			language = String.valueOf(langAPI.getDefaultLanguage().getId());

		try {
			key = key.replace(" ", "\\ ");
			MessagesTools resources = new MessagesTools();
			resources.init(ctx);
		} catch (Exception e) {
			Logger.error(this, e.toString());
		}
		String value = null;

		try {
			MessagesTools resources = new MessagesTools();
			resources.init(ctx);
			value = resources.get(key, args);
		} catch (Exception e) {
			Logger.error(this, e.toString());
		}
		return (value == null) ? "" : value;
	}

	public String get(String key, String languageId) {
		String value = null;
		try {
			Language lang = langAPI.getLanguage(languageId);
			value = langAPI.getStringKey(lang, key);

			if ((!UtilMethods.isSet(value) || value.equals(key))
					&& Config
							.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE")) {
				lang = langAPI.getDefaultLanguage();
				value = langAPI.getStringKey(lang, key);
			}
		} catch (Exception e) {
			Logger.error(this, e.toString());
		}

		return (value == null) ? "" : value;

	}

	public int getInt(String key) {
		String language = null;
		if (language == null)
			language = (String) request.getSession().getAttribute(
					com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
		if (language == null)
			language = String.valueOf(langAPI.getDefaultLanguage().getId());

		return getInt(key, language);
	}

	public int getInt(String key, String languageId) {
		int value = 0;
		try {
			Language lang = langAPI.getLanguage(languageId);
			value = langAPI.getIntKey(lang, key);
		} catch (Exception e) {
			Logger.error(this, e.toString());
		}

		return value;

	}

	public float getFloat(String key) {
		String language = null;
		if (language == null)
			language = (String) request.getSession().getAttribute(
					com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
		if (language == null)
			language = String.valueOf(langAPI.getDefaultLanguage().getId());
		return getFloat(key, language);
	}

	public float getFloat(String key, String languageId) {
		float value = 0;
		try {
			Language lang = langAPI.getLanguage(languageId);
			value = langAPI.getFloatKey(lang, key);
		} catch (Exception e) {
			Logger.error(this, e.toString());
		}

		return value;

	}

	public boolean getBoolean(String key) {
		String language = null;
		if (language == null)
			language = request.getParameter("languageId");
		if (language == null)
			language = (String) request.getSession().getAttribute(
					com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
		if (language == null)
			language = String.valueOf(langAPI.getDefaultLanguage().getId());
		return getBoolean(key, language);
	}

	public boolean getBoolean(String key, String languageId) {
		boolean value = false;
		try {
			Language lang = langAPI.getLanguage(languageId);
			value = langAPI.getBooleanKey(lang, key);
		} catch (Exception e) {
			Logger.error(this, e.toString());
		}

		return value;

	}

	public String getFromUserLanguage(String key) {
		User user1=null;
		try {
			user1 = com.liferay.portal.util.PortalUtil.getUser(this.request);
		} catch (PortalException e) {
			Logger.error(this, e.toString());
			
		} catch (SystemException e) {
			Logger.error(this, e.toString());
		}
		String message=null;
		try {
			message=LanguageUtil.get(user1, key);
		} catch (LanguageException e) {
			Logger.error(this, e.toString());
		}
		return message;
	}
	
	public String getFromSessionLanguage(String key) throws LanguageException {
	    String language = null;
        if (language == null)
            language = request.getParameter("languageId");
        if (language == null)
            language = (String) request.getSession().getAttribute(
                    com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
        if (language == null)
            language = String.valueOf(langAPI.getDefaultLanguage().getId());
        Language lang=APILocator.getLanguageAPI().getLanguage(language);
        return LanguageUtil.get(LanguageUtil.getLocale(lang.getLanguageCode()),key);
	}
	
}
   
    
