package com.dotmarketing.viewtools;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class GlobalVariableWebAPI implements ViewTool {
	
    private HttpServletRequest request;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

    public void init(Object obj) {
        ViewContext context = (ViewContext) obj;
        this.request = context.getRequest();

    }
    
    private int getCurrentLanguageId () {
        int language;
        String languageSt = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
        language = Integer.parseInt(languageSt);
        return language;
    }
    
    public String get(String property) {
        int languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getStringKey(lang, property);
    }

    public int getInt(String property) {
        int languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getIntKey(lang, property);
    }

    public boolean getBoolean(String property) {
        int languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getBooleanKey(lang, property);
    }

    public float getFloat(String property) {
        int languageId = getCurrentLanguageId();
        Language lang = langAPI.getLanguage(languageId);
        return langAPI.getFloatKey(lang, property);
    }
}