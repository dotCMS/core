package com.dotcms.rendering.velocity.viewtools;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
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

    /**
     * Resolves the language the same way the other viewtools that need it do, so the session, a
     * language set as a request attribute and the time-machine (tm_lang) language are all honoured,
     * and falls back to the default language when there is nothing to resolve.
     *
     * @return the current language id, as a string
     */
    private String currentLanguageId() {
        return String.valueOf(WebAPILocator.getLanguageWebAPI().getLanguage(request).getId());
    }
    
    public String get(String key) {
    	return get(key, currentLanguageId());
    }
    
    
    public String get(String key, List args) {
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
        final String language = currentLanguageId();
        
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
        final String language = currentLanguageId();
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
        // an explicit languageId parameter still wins over the resolved language
        final String languageId = request.getParameter("languageId");
        final String language = UtilMethods.isSet(languageId) ? languageId : currentLanguageId();
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
