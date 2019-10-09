package com.dotmarketing.portlets.languagesmanager.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.SessionMessages;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.BeanUtils;

/**
 * This Struts action will handle all the requests related to adding, updating or deleting a system
 * Language. This action is called from the Languages editing page.
 * 
 * @author alex
 * @author davidtorresv
 * @version N/A
 * @since Mar 22, 2012
 *
 */
public class EditLanguageAction extends DotPortletAction {
	
	private final LanguageAPI languageAPI;

	/**
	 * Default class constructor.
	 */
	public EditLanguageAction() {
        this(APILocator.getLanguageAPI());
    }
	
	@VisibleForTesting
    public EditLanguageAction(final LanguageAPI languageAPI) {
	    this.languageAPI = APILocator.getLanguageAPI();
    }

    /**
     * Handles all the actions associated to a system language.
     *
     * @param mapping - Contains all the mapping information for this Action, as specified in the
     *        Struts configuration file.
     * @param form - The form containing the information selected by the user in the UI.
     * @param config - The configuration parameters for this portlet.
     * @param req - The HTTP Request wrapper.
     * @param res - The HTTP Response wrapper.
     * @throws Exception An error occurred when editing a field.
     */
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

    /**
     * Retrieves the language from the database if it comes from the Languages page, or from the
     * form to save it.
     * 
     * @param req - The HTTP Request wrapper.
     * @param res - The HTTP Response wrapper.
     * @param config - The configuration parameters for this portlet.
     * @param form - The form containing the information selected by the user in the UI.
     * @param languageId - The ID of the language that will be saved/updated.
     * @throws Exception An error occurred when retrieving a language.
     */
    private void _retrieveLanguage(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, String languageId)
    throws Exception {
        Language language = languageAPI.getLanguage(languageId);
        if(language == null) {
        	language = new Language();
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE, language);
    }
    
    /**
     * Saves or updates the specified language.
     * 
     * @param req - The HTTP Request wrapper.
     * @param res - The HTTP Response wrapper.
     * @param config - The configuration parameters for this portlet.
     * @param form - The form containing the information selected by the user in the UI.
     * @throws Exception An error occurred when saving a language.
     */
   private void _save(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		Language language = (Language) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LANGUAGE) ;
		BeanUtils.copyProperties(language,form);
        if (UtilMethods.isSet(language.getLanguageCode()) && UtilMethods.isSet(language.getLanguage())) {
			try{
				languageAPI.saveLanguage(language);
			} catch(Exception e ){
				SessionMessages.add(req,"message", "message.languagemanager.languagenotsaved");
				throw new SQLException();
			}
			SessionMessages.add(req,"message", "message.languagemanager.language_save");
			_sendToReferral(req, res, StringPool.BLANK);
		}else{
			SessionMessages.add(req,"message", "message.languagemanager.language_should_not_be_empty");
			setForward(req, "portlet.ext.languagesmanager.edit_language");
		}
	}

   /**
    * Deletes the specified language.
    * 
    * @param req - The HTTP Request wrapper.
    * @param res - The HTTP Response wrapper.
    * @param config - The configuration parameters for this portlet.
    * @param form - The form containing the information selected by the user in the UI.
    * @param languageId - The ID of the language that will be deleted.
    * @throws Exception An error occurred when retrieving a language.
    */
	private void _delete(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, String languageId){
		Language language = languageAPI.getLanguage(languageId);
		try{
			languageAPI.deleteLanguage(language);
			SessionMessages.add(req,"message", "message.language.deleted");
        	Logger.debug(this, "deleted");
		}catch (Exception e){
			SessionMessages.add(req,"message", "message.language.content");
		}
	}

}
