package com.dotmarketing.cms.forgotpassword.action;

import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.forgotpassword.struts.ForgotPasswordForm;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.myaccount.action.AccountActivationAction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;


/**
 * This is the struts action that handles the forgot password process
 *
 * @author David Torres
 * @version $Revision: 1.5 $
 *
 */
public class ForgotPasswordAction extends DispatchAction {
	
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	
	/**
	 * Send you to the forgot password page unless you are coming form and reset password 
	 * link gotten in your email that case it sends you to the reset password page
	 */
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ActionForward af = (mapping.findForward("forgotPasswordPage"));
		ForgotPasswordForm form = (ForgotPasswordForm) lf;
		
		if (UtilMethods.isSet(form.getAccKey()) && Config.getBooleanProperty("USE_RESET_PASSWORD_EMAIL")) {			
			request.setAttribute("email", form.getEmail());
			af = (mapping.findForward("resetPasswordPage"));
		}
		
		if (Config.getBooleanProperty("USE_CHALLENGE_QUESTION"))
		{
    		 af = new ActionForward(mapping.findForward("challengeQuestionPage").getPath() + "?emailAddress=" + form.getEmail());
    	}
		
		return af;
	}
	
	/**
	 * Resets the password with a random generated password unless you have set the USE_CHALLENGE_QUESTION
	 * property to true in that case it sends you to the challenge question verification page
	 * or if you have the USE_RESET_PASSWORD_EMAIL set to true it sends the password reset link
	 * via email to the user 
	 * 
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public ActionForward forgotPassword(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		//setting some variables
		ActionForward af = (mapping.findForward("forgotPasswordPage"));
		ForgotPasswordForm form = (ForgotPasswordForm) lf;
		request.setAttribute("email", form.getEmail());

		String referrer = null;
		if (request.getAttribute("referrer") != null && !request.getAttribute("referrer").toString().equalsIgnoreCase(""))
		{
			referrer = (String)request.getAttribute("referrer");
		}
		else if (request.getParameter("referrer") != null && !request.getParameter("referrer").toString().equalsIgnoreCase(""))
		{
			referrer = (String)request.getParameter("referrer");
		} 
		
		try {
			//Validating the input
			if(!APILocator.getUserAPI().userExistsWithEmail(form.getEmail())) {
				ActionMessages aes = new ActionErrors();
				aes.add(Globals.ERROR_KEY, new ActionMessage("error.user.email.doesnt.exists"));
				saveErrors(request, aes);
				return af;
			}
		} catch (DotDataException e) {
			ActionMessages aes = new ActionErrors();
			aes.add(Globals.ERROR_KEY, new ActionMessage("error.user.email.doesnt.exists"));
			saveErrors(request, aes);
			return af;
		} catch (NoSuchUserException e) {
			ActionMessages aes = new ActionErrors();
			aes.add(Globals.ERROR_KEY, new ActionMessage("error.user.email.doesnt.exists"));
			saveErrors(request, aes);
			return af;
		}
		
		//If the user doesn't exists
		User user = APILocator.getUserAPI().loadByUserByEmail(form.getEmail(), APILocator.getUserAPI().getSystemUser(), false);
		if(user.isNew()){
			ActionErrors aes = new ActionErrors();
			aes.add(Globals.ERROR_KEY, new ActionMessage("error.user.email.doesnt.exists"));
			saveMessages(request.getSession(), aes);
			
			if(UtilMethods.isSet(referrer)) {
	        	af = new ActionForward(referrer);
	        	af.setRedirect(true);
	        	return af;
			} else 
				return af;
			
		}

		//If the account is not active
		if(!user.isActive()){
			
			ActionMessages aes = new ActionErrors();
			aes.add(Globals.ERROR_KEY, new ActionMessage("error.user.is.not.active"));
			saveErrors(request, aes);
			
        	af = mapping.findForward("resendActivationPage");
        	return af;
			
		}
		
		
		if (Config.getBooleanProperty("USE_CHALLENGE_QUESTION")) {
		
			request.setAttribute("email", form.getEmail());
			form.setAccKey(PublicEncryptionFactory.encryptString(user.getUserId()));
			return mapping.findForward("challengeQuestionPage");
		
		} else if (Config.getBooleanProperty("USE_RESET_PASSWORD_EMAIL")) {			
		
			request.setAttribute("email", form.getEmail());
			return sendResetPassword(mapping, lf, request, response);

		} else {
			
			//if we have some errors

			String pass = PublicEncryptionFactory.getRandomPassword();
			user.setPassword(PublicEncryptionFactory.digestString(pass));
			APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
			Host host = hostWebAPI.getCurrentHost(request);
        	Company company = PublicCompanyFactory.getDefaultCompany(); 
			
			HashMap<String, Object> parameters = new HashMap<String, Object> ();
			parameters.put("subject", "Your " + host.getHostname() + " Password");
			parameters.put("password", pass);
			parameters.put("emailTemplate", Config.getStringProperty("FORGOT_PASSWORD_EMAIL_TEMPLATE"));
			parameters.put("to", user.getEmailAddress());
			parameters.put("from", company.getEmailAddress());
			
			EmailFactory.sendParameterizedEmail(parameters, null, host, user);

			ActionMessages msg = new ActionMessages();
			msg.add(Globals.MESSAGE_KEY, new ActionMessage("message.forgot.password.email.sent"));
			request.setAttribute(Globals.MESSAGE_KEY, msg);
			
			af = (mapping.findForward("passwordChangeConfirmationPage"));
	
			if(UtilMethods.isSet(referrer)) {
	        	af = new ActionForward(referrer);
	        	af.setRedirect(true);
			} 
	        return af;
		}
	}
	
	//USE_CHALLENGE_QUESTION
	
	/**
	 * This method verifies that the correct challenge question has been answered by the user and then 
	 * sends the random generated password through email
	 */
    public ActionForward verifyChallengeQuestion(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	ForgotPasswordForm fpf = (ForgotPasswordForm) lf;
		String acckeyCrypted = fpf.getAccKey();
		String acckey = acckeyCrypted;
		
		try {
			acckey = PublicEncryptionFactory.decryptString(acckeyCrypted);
		} catch (Exception e) {
		}
		
		String referrer = null;
		if (request.getAttribute("referrer") != null && !request.getAttribute("referrer").toString().equalsIgnoreCase("")) {
			referrer = (String)request.getAttribute("referrer");
		} else if (request.getParameter("referrer") != null && !request.getParameter("referrer").toString().equalsIgnoreCase("")) {
			referrer = (String)request.getParameter("referrer");
		}
		
        try {
        	
        	User user = APILocator.getUserAPI().loadByUserByEmail(acckey, APILocator.getUserAPI().getSystemUser(), false);
        	Company company = PublicCompanyFactory.getDefaultCompany();
        	
    		request.setAttribute("email", user.getEmailAddress());
        	
    		String email = UtilMethods.isSet(request.getParameter("email"))?request.getParameter("email"):user.getEmailAddress();
        	UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
        	String challengeQuestionAnswer = request.getParameter("challengeQuestionAnswer");
        	
        	if (userProxy.getChallengeQuestionAnswer().equalsIgnoreCase(challengeQuestionAnswer)) {
				
				String pass = PublicEncryptionFactory.getRandomPassword();
				user.setPassword(PublicEncryptionFactory.digestString(pass));
				APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
				Host host = hostWebAPI.getCurrentHost(request);
				try {
					HashMap<String, Object> parameters = new HashMap<String, Object> ();
					parameters.put("subject", "Your " + host.getHostname() + " Password");
					parameters.put("password", pass);
					parameters.put("emailTemplate", Config.getStringProperty("CHALLENGE_QUESTION_EMAIL_TEMPLATE"));
					parameters.put("to", email);
					parameters.put("from", company.getEmailAddress());
					
					EmailFactory.sendParameterizedEmail(parameters, null, host, user);
					
					ActionMessages msg = new ActionMessages();
		            msg.add(Globals.MESSAGE_KEY, new ActionMessage("message.challenge_question.answer_successful", email));
		            saveMessages(request.getSession(), msg);
		            if(UtilMethods.isSet(referrer)) {
			        	return (new ActionForward(referrer + "?" + request.getQueryString()));
		            } else {
		            	return mapping.findForward("passwordChangeConfirmationPage");
		            }
				} catch (Exception e) {
		        	ActionMessages msg = new ActionMessages();
		            msg.add(Globals.ERROR_KEY, new ActionMessage("error.send_email"));
		            request.setAttribute(Globals.ERROR_KEY, msg);
		            return mapping.findForward("challengeQuestionPage");
				}
        	} else {
        		ActionMessages msg = new ActionMessages();
	            msg.add(Globals.ERROR_KEY, new ActionMessage("message.challenge_question.answer_failure"));
	            request.setAttribute(Globals.ERROR_KEY, msg);
	            fpf.setAccKey(PublicEncryptionFactory.encryptString(user.getUserId()));
	            fpf.setEmail(user.getEmailAddress());
	            
	            return mapping.findForward("challengeQuestionPage");
        	}
		} catch (Exception e) {
			Logger.debug(this, "Failed - Redirecting to: loginPage");
	        ActionErrors errors = new ActionErrors();
	        errors.add(Globals.ERROR_KEY, new ActionMessage("error.send_email"));
	        request.setAttribute(Globals.ERROR_KEY, errors);
	        
	        return mapping.findForward("loginPage");
		}
    }
	
	
	//USE_RESET_PASSWORD_LINK actions
	
	/**
	 * sends a new account activation email to the user registered email inbox
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ActionForward sendResetPassword(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String emailAddress = request.getParameter("email") == null?(String)request.getAttribute("email"):request.getParameter("email");

		ActionMessages am = new ActionMessages();

		User user = APILocator.getUserAPI().loadByUserByEmail(emailAddress, APILocator.getUserAPI().getSystemUser(), false);
		if (!user.isNew()) {

			// the user is active
			// sending Reset Password Email
			sendResetPasswordEmail(user, request);
	
			am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("send.reset.password.email.confirmation"));
			saveMessages(request.getSession(), am);
			return mapping.findForward("emailSentConfirmationPage");

		}

		// the user does not exists
		am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.user.not.exist"));
		saveMessages(request, am);
		return mapping.findForward("loginAction");
	}

	private void sendResetPasswordEmail(User user, HttpServletRequest request) throws PortalException, SystemException, DotDataException, DotSecurityException {
		Host host = hostWebAPI.getCurrentHost(request);
    	Company company = PublicCompanyFactory.getDefaultCompany();
    	Date date = UtilMethods.addDays(new Date(), 7);
    	String linkparam = user.getUserId() + "##" + UtilMethods.dateToJDBC(date);
    	Logger.debug(ForgotPasswordAction.class, "linkparam="+linkparam);
    	String linkparamEncrypted = PublicEncryptionFactory.encryptString(linkparam);
    	Logger.debug(ForgotPasswordAction.class, "linkparamEncrypted="+linkparamEncrypted);

		HashMap<String, Object> parameters = new HashMap<String, Object> ();
		parameters.put("subject", company.getName() + " Reset Password Link");
		parameters.put("linkurl", UtilMethods.encodeURL(linkparamEncrypted));
		parameters.put("emailTemplate", Config.getStringProperty("RESET_PASSWORD_LINK_EMAIL_TEMPLATE"));
		parameters.put("to", user.getEmailAddress());
		parameters.put("from", company.getEmailAddress());
		parameters.put("company", company.getName());

		try {
			EmailFactory.sendParameterizedEmail(parameters, null, host, user);
		}
		catch (Exception e) {
			Logger.error(ForgotPasswordAction.class, "Error sending Reset Password Email");
		}
	}

	/**
	 * updates the password of a specific user
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public ActionForward resetPassword(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		ForgotPasswordForm form = (ForgotPasswordForm)lf;
		
		String acckeyCrypted = form.getAccKey();
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
		
			// the user is active
			// validating reset password email link

			if (linkExpirationDate.after(new Date())) {

				// updating user password
				if (!Validator.validate(request, lf, mapping))
					return mapping.findForward("resetPasswordPage");

				user.setPassword(PublicEncryptionFactory.digestString(form.getNewPassword()));
				user.setPasswordEncrypted(true);

				APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);

				Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
				if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
					LoginFactory.doLogin(user.getEmailAddress(), form.getNewPassword(), false, request, response);
				} else {
					LoginFactory.doLogin(user.getUserId(), form.getNewPassword(), false, request, response);
				}

				am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.forgot.password.password.updated"));
				saveMessages(request, am);
				return mapping.findForward("passwordChangeConfirmationPage");
			}
			else {
				// the reset password email link has expired, 
				// it's needed to resend the reset password email
				return sendResetPassword(mapping, lf, request, response);
			}

		}

		// the user does not exists
		am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.user.not.exist"));
		saveMessages(request, am);
		return mapping.findForward("loginPage");
	}
	
}
