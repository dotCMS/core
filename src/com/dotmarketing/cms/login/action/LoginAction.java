package com.dotmarketing.cms.login.action;

import java.util.List;

import javax.servlet.http.Cookie;
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

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

/**
 *
 */
public class LoginAction extends DispatchAction {

    public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

    	Logger.debug(this, "Locale: " + request.getSession().getAttribute(Globals.LOCALE_KEY));

        /*This was created for CSU-604
         * This code let it send messages of warning between pages, when
         * the session or request messages doesn't work
         * */
        if(UtilMethods.isSet(request.getParameter("message"))){
        	ActionMessages message = new ActionMessages();
			message.add("message", new ActionMessage(request.getParameter("message")));
			saveMessages(request, message);
        }else if(UtilMethods.isSet(request.getParameter("error"))){
        	ActionMessages errors = new ActionMessages();
			errors.add("errors", new ActionMessage(request.getParameter("error")));
			saveErrors(request, errors);
        }

        ActionForward af = (mapping.findForward("loginPage"));

        return af;
    }

    public ActionForward login(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        LoginForm form = (LoginForm) lf;

        /**
         * referrer can be used to have diferent login forms in diferent pages
         * so referer has to be set (as a hidden input) in the login form page to return there in case of an error or
         * success (the sucess url can be overriden by setting the session REDIRECT_AFTER_LOGIN property in the login form as well)
         */
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

	        if (LoginFactory.doLogin(form, request, response)) {

	        	if(!UtilMethods.isSet(referrer))
	        		referrer = "/";

	            User u = (User) request.getSession().getAttribute(WebKeys.CMS_USER);

	            List<Role> userRoles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(u.getUserId());
	            Role defaultRole = com.dotmarketing.business.APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_VIEWER_ROLE"));
	            if (!userRoles.contains(defaultRole)) {
	            	com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(defaultRole.getId(), u);
	            }

	            UserProxy userproxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false);
	            if (UtilMethods.isSet(userproxy.getLongLivedCookie())) {
	            	//reset cookie in request
	            	Cookie cookie = UtilMethods.getCookie(request.getCookies(), WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
	            	if (cookie != null) {
	            		cookie.setMaxAge(-1);
	            		cookie.setPath("/");
	                    response.addCookie(cookie);
	            	}
	            }
	            else {
	        		String _dotCMSID = "";
	        		if(!UtilMethods.isSet(UtilMethods.getCookieValue(request.getCookies(),
	        				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE))) {
	        			Cookie idCookie = CookieUtil.createCookie();

	        		}
        			_dotCMSID = UtilMethods.getCookieValue(request.getCookies(),
        					com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
	        		userproxy.setLongLivedCookie(_dotCMSID);

	            }

	            request.getSession().removeAttribute(WebKeys.PENDING_ALERT_SEEN);

	            if (request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN) != null) {
	                String redir = (String) request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
	                request.removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
	                Logger.debug(this.getClass(), "redirecting after account creation: " + redir);
	                ActionForward af = new ActionForward(redir);
	                af.setRedirect(true);
	                return af;
	            }

	    		ActionMessages msg = new ActionMessages();
	            msg.add(Globals.MESSAGE_KEY, new ActionMessage("message.Login.Successful"));
	            request.setAttribute(Globals.MESSAGE_KEY, msg);

	            ActionForward af = new ActionForward(referrer);
	            af.setRedirect(true);
	            return af;
	        }
	        else if (isUserInactive(form, request)) {
        		return mapping.findForward("resendActivationPage");
	        }

	        Logger.debug(this, "Failed login redirecting to: " + referrer);
	        ActionErrors errors = new ActionErrors();
	        errors.add(Globals.ERROR_KEY, new ActionMessage("errors.password.mismatch"));
	        request.getSession().setAttribute(Globals.ERROR_KEY, errors);

	        if(referrer != null && !referrer.equals("/")) {
	        	ActionForward af = new ActionForward(referrer);
	        	af.setRedirect(true);
	        	return af;
	        } else {
	        	if (!Config.getBooleanProperty("USE_CHALLENGE_QUESTION")) {
	    	        if(referrer != null && !referrer.equals("/")) {
	    	        	ActionForward af = new ActionForward(referrer);
	    	        	af.setRedirect(true);
	    	        	return af;
	    	        } else
	    	        	return mapping.findForward("loginPage");
	        	} else {
	        		User user = null;
	            	Company company = PublicCompanyFactory.getDefaultCompany();
	        		if (company.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	        			user = APILocator.getUserAPI().loadByUserByEmail(form.getUserName().toLowerCase(), APILocator.getUserAPI().getSystemUser(), false);
	            	} else {
	            		user = APILocator.getUserAPI().loadUserById(form.getUserName().toLowerCase(),APILocator.getUserAPI().getSystemUser(),false);
	            	}
	        		ActionForward af = new ActionForward(mapping.findForward("challengeQuestionPage").getPath() + "?emailAddress=" + user.getEmailAddress());

	        		return af;
	        	}
	        }

		} catch (NoSuchUserException e) {
			Logger.debug(this, "Failed - User does not exist - login redirecting to: loginPage");
	        ActionErrors errors = new ActionErrors();
	        errors.add(Globals.ERROR_KEY, new ActionMessage("errors.user.not.exist"));
	        request.setAttribute(Globals.ERROR_KEY, errors);
	        //return to login page showing message the user doesn't exist
	        if(referrer != null && !referrer.equals("/")) {
	        	ActionForward af = new ActionForward(referrer);
	        	af.setRedirect(true);
	        	return af;
	        } else
	        	return mapping.findForward("loginPage");
		}

    }

    /**
     * validates if an user exists and its status is inactive
     * this method is for Knight Foundation only
     * @param form
     * @param request
     * @return true if the user exists but it's inactive
     * @throws NoSuchUserException
     */
    private boolean isUserInactive(LoginForm form, HttpServletRequest request) throws NoSuchUserException {
        try {

        	String userName = form.getUserName();

            Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
            User user = null;

            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
            } else {
            	user = APILocator.getUserAPI().loadUserById(userName,APILocator.getUserAPI().getSystemUser(),false);
            }

            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
            	throw new NoSuchUserException();
            }

            if (!user.isActive()) {
				// re-sending activation account link
				request.setAttribute("userId", user.getUserId());
				return true;
            }

        } catch (NoSuchUserException e) {
        	throw e;
        } catch (Exception e) {
            Logger.debug(LoginFactory.class, "userExistsButInactive validation Failed" + e);
        }
        return false;

    }
}
