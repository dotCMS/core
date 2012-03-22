package com.dotmarketing.portlets.user.action;

import java.lang.reflect.InvocationTargetException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.usermanager.struts.UserManagerForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

/*
 * @author Oswaldo Gallango
 */
public class EditUserProxyAction extends DotPortletAction {

	public static boolean debug = false;

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		HibernateUtil.startTransaction();
		String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);
		String referer = req.getParameter("referer");
		UserManagerForm userForm = (UserManagerForm) form;
		req.setAttribute(WebKeys.USERMANAGER_EDIT_FORM, form);

		Logger.debug(this, "Saving UserInfo");

		
			
		try {
			_updateUserProxy(form, req, res);

		} catch (Exception e) {
			_handleException(e, req);
		}

		_sendToReferral(
				req,
				res,
				"/c/portal/layout?p_l_id=1&p_p_id=EXT_USERMANAGER&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_USERMANAGER_struts_action=%2Fadmin%2Fedit_user_profile&_EXT_USERMANAGER_p_u_e_a="
						+ userForm.getEmailAddress());
		// setForward(req, "portlet.my_account.edit_profile");
		HibernateUtil.commitTransaction();
	}

	/* Private Methods */

	private void _updateUserProxy(ActionForm form, ActionRequest req, ActionResponse res) {

		UserManagerForm userForm = new UserManagerForm();
		try {
			BeanUtils.copyProperties(userForm, form);

			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userForm.getUserID(),APILocator.getUserAPI().getSystemUser(), false);
			if (!userForm.getPrefix().equals("other"))
				userProxy.setPrefix(userForm.getPrefix());
			else
				userProxy.setPrefix(userForm.getOtherPrefix());

			userProxy.setSuffix(userForm.getSuffix());
			userProxy.setTitle(userForm.getTitle());
			userProxy.setSchool(userForm.getSchool());
			userProxy.setGraduation_year(userForm.getGraduation_year());
			userProxy.setCompany(userForm.getCompany());
			userProxy.setWebsite(userForm.getWebsite());
			userProxy.setHowHeard(userForm.getHowHeard());
			userProxy.setChapterOfficer(userForm.getChapterOfficer());

			HibernateUtil.saveOrUpdate(userProxy);

		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

}
