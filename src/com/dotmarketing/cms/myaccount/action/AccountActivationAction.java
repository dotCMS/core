package com.dotmarketing.cms.myaccount.action;

import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class AccountActivationAction extends DispatchAction {

	@SuppressWarnings("unchecked")
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception {
		String userId = request.getParameter("userId");
		if (!UtilMethods.isSet(userId)) {
			userId = (String) request.getSession().getAttribute("userId");
		}

		if(UtilMethods.isSet(userId)) {
			// resending activation account link
			request.setAttribute("userId", userId);
			return mapping.findForward("resendPage");
		}
		
		return activateAccount(mapping, lf, request, response);
	}

	/**
	 * activates a lightweight user account after the activation link has been clicked
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ActionForward activateAccount(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception {
		String acckeyCrypted = request.getParameter("acckey");
    	Logger.debug(AccountActivationAction.class, "acckeyCrypted="+acckeyCrypted);
		String acckey = PublicEncryptionFactory.decryptString(acckeyCrypted);
    	Logger.debug(AccountActivationAction.class, "acckey="+acckey);
		StringTokenizer strTok = new StringTokenizer(acckey, "##");

		String userId = strTok.nextToken();
		String linkExpirationDateStr = strTok.nextToken();

		Date linkExpirationDate = UtilMethods.jdbcToDate(linkExpirationDateStr);
		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		ActionMessages am = new ActionMessages();

		if (!user.isNew()) {

			// the user exists
			if (!user.isActive()) {
	
				if (linkExpirationDate.after(new Date())) {
					user.setActive(true);
					APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
		
					//Logging in the user
			        Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
			        if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
			        	LoginFactory.doLogin(user.getEmailAddress(), user.getPassword(), false, request, response);
			        } else {
			        	LoginFactory.doLogin(user.getUserId(), user.getPassword(), false, request, response);
			        }
					
					am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.account.user.activated"));
					saveMessages(request.getSession(), am);
					ActionForward forward = mapping.findForward("confirmation");
					return forward;
				}
				else {
					// resending activation account link
					request.setAttribute("userId", user.getUserId());
					return mapping.findForward("resendPage");
				}
			}
			else {
				am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.account.user.already.active"));
				saveMessages(request.getSession(), am);
				ActionForward forward = mapping.findForward("confirmation");
				return forward;
			}
		}

		// the user does not exists
		am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.user.not.exist"));
		saveMessages(request.getSession(), am);
		return mapping.findForward("loginPage");
	}

	/**
	 * resends a new account activation email to the user registered email inbox
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ActionForward resendActivationEmail(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String userId = request.getParameter("userId");
		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

		// sending Account Activation Email
		sendActivationAccountEmail(user, request);

		//make the redirect
		ActionMessages am = new ActionMessages();
		am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.account.activation.email.sent"));
		saveMessages(request.getSession(), am);
		return mapping.findForward("confirmation");
	}

	/**
	 * sends an account activation email to the user registered email inbox 
	 * @param user user to activate
	 * @param request
	 */
	public static void sendActivationAccountEmail(User user, HttpServletRequest request) {
		try {
			HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	
			Host host = hostWebAPI.getCurrentHost(request);
	    	Company company = PublicCompanyFactory.getDefaultCompany();
	    	Date date = UtilMethods.addDays(new Date(), 7);
	    	String linkparam = user.getUserId() + "##" + UtilMethods.dateToJDBC(date);
	    	Logger.debug(AccountActivationAction.class, "linkparam="+linkparam);
	    	String linkparamEncrypted = PublicEncryptionFactory.encryptString(linkparam);
	    	Logger.debug(AccountActivationAction.class, "linkparamEncrypted="+linkparamEncrypted);
	
			HashMap<String, Object> parameters = new HashMap<String, Object> ();
			parameters.put("subject", company.getName() + " Activation Account Link");
			parameters.put("linkurl", UtilMethods.encodeURL(linkparamEncrypted));
			parameters.put("emailTemplate", Config.getStringProperty("ACTIVATION_LINK_EMAIL_TEMPLATE"));
			parameters.put("to", user.getEmailAddress());
			parameters.put("from", company.getEmailAddress());
			parameters.put("company", company.getName());

			EmailFactory.sendParameterizedEmail(parameters, null, host, user);
		}
		catch (Exception e) {
			Logger.error(AccountActivationAction.class, "Error sending Activation Account Email");
		}
	}

}
