/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.action;

import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.Action;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.rest.api.v1.authentication.DotInvalidTokenException;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.*;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.portal.ejb.UserManagerImpl;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.*;
import com.liferay.util.CookieUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.ParamUtil;
import com.liferay.util.Validator;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.SessionMessages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.dotmarketing.util.CookieUtil.createJsonWebTokenCookie;

/**
 * <a href="LoginAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Changer
 * @version $Revision: 1.4 $
 *
 */
public class LoginAction extends Action {

	private final LoginService loginService =
			LoginServiceFactory.getInstance().getLoginService();

	/**
	 * Determines the action to execute based on the command issued by the user.
	 * 
	 * @param mapping
	 *            - The mapping definitions for this Struts action.
	 * @param form
	 *            - The HTML form with the information sent by the user.
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @param res
	 *            - The {@link HttpServletResponse} object.
	 */
    public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest req,
			HttpServletResponse res)
		throws Exception {
	    
	    SessionErrors.clear(req);
	    SessionMessages.clear(req);
		HttpSession ses = req.getSession();

		String cmd = req.getParameter("my_account_cmd");

		if ((cmd != null) && (cmd.equals("auth"))) {
			try {
				_login(req, res);
				
				User user = UserLocalManagerUtil.getUserById((String) ses.getAttribute(WebKeys.USER_ID));
				List<Layout> userLayouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
				if ((userLayouts == null) || (userLayouts.size() == 0) || !UtilMethods.isSet(userLayouts.get(0).getId())) {
					new LogoutAction().execute(mapping, form, req, res);
					throw new RequiredLayoutException();
				}
				
				Layout layout = userLayouts.get(0);
				List<String> portletIds = layout.getPortletIds();
				String portletId = portletIds.get(0);
				java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
				params.put("struts_action",new String[] {"/ext/director/direct"});
				String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(req,layout.getId(),WindowState.MAXIMIZED.toString(),params, portletId);
				ses.setAttribute(com.dotmarketing.util.WebKeys.DIRECTOR_URL, directorURL);
				

				// Touch protected resource
				return mapping.findForward("/portal/touch_protected.jsp");
				
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof AuthException ||
					e instanceof NoSuchUserException ||
					e instanceof UserEmailAddressException ||
					e instanceof UserIdException ||
					e instanceof UserPasswordException ||
					e instanceof RequiredLayoutException ||
					e instanceof UserActiveException) {

					SessionErrors.add(req, e.getClass().getName());
					SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as " + ParamUtil.getString(req, "my_account_login").toLowerCase() + " has been made from IP: " + req.getRemoteAddr());
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);
					SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as " + ParamUtil.getString(req, "my_account_login").toLowerCase() + " has been made from IP: " + req.getRemoteAddr());
					return mapping.findForward(Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals("send"))) {
			try {
				_sendPassword(req);
			}
			catch (Exception e) {
				if(e != null &&
						e instanceof NoSuchUserException){
					
					//If the user doesn't exist but property is true, we need to display the error.
					//If the user doesn't exist but property is false, wee need to display success.
					boolean displayNotSuchUserError = 
							Config.getBooleanProperty("DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD", false);
					
					if(displayNotSuchUserError){
						SessionErrors.add(req, e.getClass().getName());
					} else {
						SecurityLogger.logInfo(UserManagerImpl.class, 
								"User does NOT exist in the Database, returning OK message for security reasons");
						String emailAddress = ParamUtil.getString(
								req, "my_account_email_address");
						SessionMessages.add(req, "new_password_sent", emailAddress);
						
					}
					
				} else if (e != null &&
					e instanceof SendPasswordException ||
					e instanceof UserEmailAddressException) {

					SessionErrors.add(req, e.getClass().getName());
					
				} else {
					req.setAttribute(PageContext.EXCEPTION, e);

					return mapping.findForward(Constants.COMMON_ERROR);
				}
			}
		}
		else if(cmd!=null && cmd.equals("ereset")) {
		    try {
		        _resetPassword(req);
		    }
		    catch(Exception ex) {
		        req.setAttribute(PageContext.EXCEPTION, ex);
                return mapping.findForward(Constants.COMMON_ERROR);
		    }
		}
		
		return mapping.findForward("portal.login");
	}

    /**
     *
     * @param req
     * @throws Exception
     */
	private void _resetPassword(HttpServletRequest req) throws Exception {
		String userId = ParamUtil.getString(req, "my_user_id");
		String token = ParamUtil.getString(req, "token");
		
		if(!UtilMethods.isSet(userId)){
			JWTBean jwtBean  = JsonWebTokenFactory.getInstance().getJsonWebTokenService().parseToken(token);
			userId = jwtBean.getId();
			token = jwtBean.getSubject();
		}
		
        String newpass1 = ParamUtil.getString(req, "my_new_pass1");
		String newpass2 = ParamUtil.getString(req, "my_new_pass2");


		if(UtilMethods.isSet(newpass1) && UtilMethods.isSet(newpass2)) {
			// actualy change password
			if(newpass1.equals(newpass2)) {
				try {
					UserManagerFactory.getManager().resetPassword(userId, token, newpass1);
					SecurityLogger.logInfo(LoginAction.class, "User "+userId+" successful changed his password from IP:"+req.getRemoteAddr());
					SessionMessages.add(req, "reset_pass_success");
				}catch(com.dotmarketing.business.NoSuchUserException | DotSecurityException e){
					throw e;
				}catch(DotInvalidTokenException e){
					if (e.isExpired()){
						SecurityLogger.logInfo(LoginAction.class, "User "+userId+" requested password reset with expired token from IP:"+req.getRemoteAddr());
						SessionErrors.add(req, "reset_token_expired");
					}else{
						SecurityLogger.logInfo(LoginAction.class, "Attempt to reset user password ("+userId+") with wrong token. IP:"+req.getRemoteAddr());
					}
				}catch(DotInvalidPasswordException e){
					SecurityLogger.logInfo(LoginAction.class, "User "+userId+" couldn't reset password because it is invalid. From IP:"+req.getRemoteAddr());
					SessionErrors.add(req, "reset_pass_invalid_pass");
				}
			}
			else {
				SessionErrors.add(req, "reset_pass_not_match");
			}
		}
		else {
			// just show the option to reset in UI
			SecurityLogger.logInfo(LoginAction.class, "User "+userId+" successful password reset request from IP:"+req.getRemoteAddr());
			SessionMessages.add(req, "reset_ok");
		}
	}

	/**
	 * Performs the authentication process carried out through the Login page.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @param res
	 *            - The {@link HttpServletResponse} object.
	 * @throws Exception
	 *             An error occurred during the authentication process.
	 */
	private void _login(HttpServletRequest req, HttpServletResponse res)
		throws Exception {

		String login = ParamUtil.getString(req, "my_account_login").toLowerCase();
		String password = ParamUtil.getString(req, "password");
		if (Validator.isNull(password)) {
			password = ParamUtil.getString(req, "password");
		}
		
		boolean rememberMe = ParamUtil.get(req, "my_account_r_m", false);

		String userId = login;

		this.loginService.doActionLogin(userId, password, rememberMe, req, res);
	}

    /**
     * 
     * @param req
     * @throws Exception
     */
    private void _sendPassword(HttpServletRequest req) throws Exception {
		String emailAddress = ParamUtil.getString(
			req, "my_account_email_address");
		String userId = emailAddress;

		Company company = PortalUtil.getCompany(req);

		if (company.getAuthType().equals(Company.AUTH_TYPE_ID)) {			
			User user = UserLocalManagerUtil.getUserById(userId);
			emailAddress = user.getEmailAddress();
		}
		
		Locale locale = (Locale)req.getSession().getAttribute(Globals.LOCALE_KEY);

		UserManagerUtil.sendPassword(
			PortalUtil.getCompanyId(req), emailAddress, locale);

		SecurityLogger.logInfo(this.getClass(),"Email address " + emailAddress + " has request to reset his password from IP: " + req.getRemoteAddr());

		SessionMessages.add(req, "new_password_sent", emailAddress);
	}

}
