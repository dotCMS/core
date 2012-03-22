package com.dotmarketing.portlets.user.action;

import java.net.URLDecoder;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.user.factories.UserPreferencesFactory;
import com.dotmarketing.portlets.user.model.UserPreference;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;

/**
 * @author David Torres 
 */ 

public class EditUserPreferenceAction extends DotPortletAction
{
	
	public static boolean debug = false;
	
	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		
        String cmd = req.getParameter(Constants.CMD);
		
		String referer = req.getParameter("referer");

		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}

        HibernateUtil.startTransaction();

		User user = _getUser(req);
		
        try {
			_retrieveUserPreference(req, res, config, form, user);
        } catch (ActionException ae) {
        	//_handleException(ae, req);
        }

        /*
         * We are editing the workflow message
         */
        if ((cmd != null) && cmd.equals(Constants.EDIT)) {
        }
        
        if ((cmd != null) && cmd.equals(Constants.SAVE)) {
            try {
				_saveUserPreference(req, res, config, form, user);
            } catch (ActionException ae) {
                Logger.error(this, "ERROR SAVING!!!!", ae);
            }
            
        }
        
        HibernateUtil.commitTransaction();
    }

	///// ************** ALL METHODS HERE *************************** ////////

	public void _retrieveUserPreference(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
		throws Exception {
        String userId = req.getParameter("userId");
        String preference = req.getParameter("preference");
        
        UserPreference u = UserPreferencesFactory.getUserPreferenceValue(userId, preference);
        req.setAttribute(WebKeys.USER_PREFERENCE_EDIT, u);
	}
	
	public void _saveUserPreference(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
		throws Exception {
        UserPreference up = (UserPreference) req.getAttribute(WebKeys.USER_PREFERENCE_EDIT);
        
        if (up.getId()>0) {
        	up.setValue(req.getParameter("value"));
        }
        else {
        	up.setUserId(req.getParameter("userId"));
        	up.setPreference(req.getParameter("preference"));
        	up.setValue(req.getParameter("value"));
        }
        
        UserPreferencesFactory.saveUserPreference(up);
		
	}
	

}
