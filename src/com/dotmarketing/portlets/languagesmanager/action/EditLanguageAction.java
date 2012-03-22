/*
 * Created on Sep 23, 2004
 *
 */
package com.dotmarketing.portlets.languagesmanager.action;



import java.sql.SQLException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;


/**
 * @author alex
 * @author davidtorresv
 *
 */
public class EditLanguageAction extends DotPortletAction {
	
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	
	
    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
        ActionResponse res) throws Exception {
        
    	ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
    	String cmd = httpReq.getParameter(Constants.CMD);
        Logger.debug(this, "\n\n***********cmd = " + cmd + "***");
        String languageId = httpReq.getParameter("id");
        Logger.debug(this, "id ="+languageId+"*********");
        
        if(languageId != null && (!languageId.equals("")) ){
        	try {
        		_retrieveLanguage(req, res, config, form, languageId);
        	} catch (Exception e) {
        		_handleException(e, req);
        	}
    	}
        
        if((cmd != null) && cmd.equals(Constants.SAVE)) {
            try {
            	if (Validator.validate(req,form,mapping)) {
          
                    Logger.debug(this, "I'm saving");
            		_save(req, res, config, form);
            		_sendToReferral(req, res, "");
            	}
            } catch (Exception ae) {
                _handleException(ae, req);
                
            }
            
        }
        
        if((cmd != null) && cmd.equals(Constants.DELETE)) {
            try {
                Logger.debug(this, "I'm deleting");
                _delete(req, res, config, form,languageId);
            } catch (Exception ae) {
                _handleException(ae, req);
                
            }
            _sendToReferral(req, res, "");
        }
        
        /*Copy copy props from the db to the form bean */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			BeanUtils.copyProperties(form, req.getAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE));
		}
        
        setForward(req, "portlet.ext.languagesmanager.edit_language");
        
    }

    /*here I retrieve the language from the database if I come from the views languages or from the form to save it*/
    private void _retrieveLanguage(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, String languageId)
    throws Exception {
        Language language = langAPI.getLanguage(languageId);
        if(language == null)
        	language = new Language();
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE, language);
    }
    
    
   /* here I save the language if it exits or not in the database*/
   private void _save(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		Language language = (Language) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE) ;
		
		BeanUtils.copyProperties(language,form);
		try{
			langAPI.saveLanguage(language);
		}
		catch(Exception e ){
			SessionMessages.add(req,"message", "message.languagemanager.languagenotsaved");
			throw new SQLException();
		}
		SessionMessages.add(req,"message", "message.languagemanager.language_save");
		
		
	}
   /* here I delete the language from the database*/
	private void _delete(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, String languageId) {
		Language language = langAPI.getLanguage(languageId);
		langAPI.deleteLanguage(language);
        Logger.debug(this, "deleted");
	}
}