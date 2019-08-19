/*
 * Created on Sep 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.portlets.languagesmanager.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * @author alex
 */
public class EditLanguageKeysAction extends DotPortletAction {
	
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	
    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
							  ActionResponse res) throws Exception {
        String cmd = req.getParameter(Constants.CMD) == null?"edit":req.getParameter(Constants.CMD);
        
        String referer = req.getParameter("referer");
        
        //get list of languages
        try {
        	_retrieveLanguages(req, res, config, form);
        } catch (Exception e) {
            _handleException(e, req);
        }

        // create if necessary and copy from normal files to tmp files
        _checkLanguagesFiles(req, res, config, form);
        
        if(cmd.equals("save")) {
        	if(_save(req, res, config, form)) {
	        	if(UtilMethods.isSet(referer)) {
	        		_sendToReferral(req, res, referer);
	        		return;
	        	}
        	}
        }

        setForward(req, "portlet.ext.languagesmanager.edit_language_keys");
    }



	private void _retrieveLanguages(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form)
        throws Exception {
		
		Language lang = null;
		if(req.getParameter("id") != null)
			lang = langAPI.getLanguage(req.getParameter("id"));
		else
			lang = langAPI.getDefaultLanguage();

	    req.setAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE, lang);

        List<Language> list = null; // LinkedList();

        if (req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST) == null) {
            list = langAPI.getLanguages();
        } else {
            list = (List<Language>) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        }

        req.setAttribute(WebKeys.LANGUAGE_MANAGER_LIST, list);
    }


    /* check for the existence of the languages resource files, if not create that */
    private void _checkLanguagesFiles(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form)
        throws Exception {
        List<Language> list = (List<Language>) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);

        for (int i = 0; i < list.size(); i++) {
        	
        	Language lang = list.get(i);
        	langAPI.createLanguageFiles(lang);

        }
    }


    private boolean _save(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form)
        throws Exception {
		Pattern p = Pattern.compile("[A-Za-z0-9-_\\.]+");
    	Language lang = (Language) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE);
    	int numberOfKeys = Integer.parseInt(req.getParameter("keys"));
    	Map<String, String> generalKeys = new HashMap<String, String>();
    	Map<String, String> specificKeys = new HashMap<String, String>();
    	Set<String> toDeleteKeys = new HashSet<String>();
    	for(int i = 0; i < numberOfKeys; i++) {
    		String remove = req.getParameter(lang.getLanguageCode() + "-" + i + "-remove");
    		String key = req.getParameter(lang.getLanguageCode() + "-" + i + "-key");
    		if(!p.matcher(key).matches()) {
				SessionMessages.add(req, "message", "message.languagemanager.key.error");
    			return false;
    		}
    		String general = req.getParameter(lang.getLanguageCode() + "-general-" + i + "-value");
    		String specific = req.getParameter(lang.getLanguageCode() + "-" + lang.getCountryCode() + "-" + i + "-value");
    		if(remove == null) {
	    		generalKeys.put(key, general);
	    		specificKeys.put(key, specific);
    		} else {
    			toDeleteKeys.add(key);
    		}
    	}
		langAPI.saveLanguageKeys(lang, generalKeys, specificKeys, toDeleteKeys);
		MultiMessageResources messages = (MultiMessageResources) req.getAttribute(Globals.MESSAGES_KEY);
		messages.reload();
		
        SessionMessages.add(req, "message", "message.languagemanager.save");
        return true;
    }



} //end of the class
