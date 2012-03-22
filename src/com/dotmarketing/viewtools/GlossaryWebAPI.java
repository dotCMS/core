package com.dotmarketing.viewtools;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class GlossaryWebAPI implements ViewTool {
	
	private ViewContext context;
    private HttpServletRequest request;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

    public void init(Object obj) {
        this.context = (ViewContext) obj;
        this.request = context.getRequest();

    }
    
    public String get(String key) {
    	String language = null;
    	if(language == null)
    		language = (String)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);	
    	if (language == null)
    		language = String.valueOf(langAPI.getDefaultLanguage().getId());	
    	return get(key, language);
    }
    
    
    public String get(String key, List args) {
        String language = null;
    	if(language == null)
    		language = (String)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
        
        if (language == null)
            language = String.valueOf(langAPI.getDefaultLanguage().getId());
        
        try {
        	key = key.replace(" ","\\ ");
            MessagesTools resources = new MessagesTools();
            resources.init(context);
        } catch (Exception e) {
            Logger.error(this,e.toString());
        }
        String value = null;
        
        try {
            MessagesTools resources = new MessagesTools();
            resources.init(context);
            value = resources.get(key, args);
        } catch (Exception e) {
            Logger.error(this,e.toString());
        }
        return( value == null) ? "": value;
    }
    
    
    
    
    
    public String get(String key, String languageId) {    	
    	String value = null;
    	try {
           	Language lang = langAPI.getLanguage(languageId);
   			value = langAPI.getStringKey(lang, key);

    		if((!UtilMethods.isSet(value) || value.equals(key)) && Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE")){
    			lang = langAPI.getDefaultLanguage();
    			value = langAPI.getStringKey(lang, key);
    		}
    	} catch (Exception e) {
    		Logger.error(this,e.toString());
    	}

    	return( value == null) ? "": value;
    	
    }
    
    
    
    
    public int getInt(String key) {
        String language = null;
        if(language == null)
    		language = (String)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);	
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
            Logger.error(this,e.toString());
        }

        return value;
        
    }
    
    
    public float getFloat(String key) {
        String language = null;
        if(language == null)
    		language = (String)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);	
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
            Logger.error(this,e.toString());
        }

        return value;
        
    }
    
    public boolean getBoolean(String key) {
        String language = null;
        if (language == null)
            language = request.getParameter("languageId");
        if(language == null)
    		language = (String)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);	
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
            Logger.error(this,e.toString());
        }

        return value;
        
    }
    
}