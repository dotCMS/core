package com.dotmarketing.portlets.user.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.user.struts.UserAdditionalInfoForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Martin Amaris 
 */

public class EditUserAdditionalInfoAction extends DotPortletAction {
    
    public static boolean debug = false;
    
    public void processAction(
            ActionMapping mapping, ActionForm form, PortletConfig config,
            ActionRequest req, ActionResponse res)
    throws Exception {
		UserAdditionalInfoForm userAdditionalInfoForm = (UserAdditionalInfoForm) form;
       
		String referer = req.getParameter("referer");
		
		String cmd = req.getParameter(Constants.CMD);		
		User user = _getUser(req);
		new HibernateUtil().startTransaction();
		try
		{
			if (cmd.equals(Constants.SAVE))
			{
				_saveUserAdittionalInfo(req,res,config,userAdditionalInfoForm,user);
			}
			new HibernateUtil().commitTransaction();
		}
		catch(Exception ex)
		{
			Logger.warn(this,ex.toString());
			new HibernateUtil().rollbackTransaction();
		}
//		referer += "&layer=additional_info";
		referer += "&layer=main";
		_sendToReferral(req,res,referer);
		SessionMessages.add(req,"additional_info_updated");
    }
    
    ///// ************** ALL METHODS HERE *************************** ////////
    
    public void _saveUserAdittionalInfo(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
    throws Exception {
    	UserAdditionalInfoForm userAdditionalInfoForm = (UserAdditionalInfoForm) form;

		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userAdditionalInfoForm.getUserProxy(),APILocator.getUserAPI().getSystemUser(), false);

		int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");
		for (int i=1; i<=numberGenericVariables; i++) {
			userProxy.setVar(i, userAdditionalInfoForm.getVar(i));
		}
		
		com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);
    }
    
}
