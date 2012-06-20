/*
 * Created on Oct 7, 2004
 *
 */
package com.dotmarketing.cms.login.factories;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;

/**
 * @author will
 *
 */
public class LoginFactory {

	public static String PRE_AUTHENTICATOR = PropsUtil.get("auth.pipeline.pre");

    public static boolean doLogin(LoginForm form, HttpServletRequest request, HttpServletResponse response) throws NoSuchUserException {
        return doLogin(form.getUserName(), form.getPassword(), form.isRememberMe(), request, response);

    }

    public static boolean doCookieLogin(String encryptedId, HttpServletRequest request, HttpServletResponse response) {

        try {
            String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
            User user = APILocator.getUserAPI().loadUserById(decryptedId,APILocator.getUserAPI().getSystemUser(),false);
            try {
                String userName = user.getEmailAddress();
                Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
                if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
                	userName = user.getUserId();
                }

                return doLogin(userName, user.getPassword(), true, request, response);
            } catch (Exception e) {
            	return false;
            }
        } catch (Exception e) {
            Logger.error(LoginFactory.class, "AutoLogin Failed" + e);

        }

        doLogout(request, response);

        return false;
    }

    /**
     *
     * @param userName
     * @param password
     * @param rememberMe
     * @param request
     * @param response
     * @return
     */
    public static boolean doLogin(String userName, String password, boolean rememberMe, HttpServletRequest request, HttpServletResponse response) throws NoSuchUserException {
        try {
        	User user = null;
        	boolean match = false;
        	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

        	if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getEmailAddress())){
					return false;
				}
			} else {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getUserId())){
					return false;
				}
			}

        	if ((PRE_AUTHENTICATOR != null) &&
        		(0 < PRE_AUTHENTICATOR.length()) &&
        		PRE_AUTHENTICATOR.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"))) {
        		Class ldap_auth_impl_class = Class.forName(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"));
        		Authenticator ldap_auth_impl = (Authenticator) ldap_auth_impl_class.newInstance();
        		int auth = 0;

    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
    				auth = ldap_auth_impl.authenticateByEmailAddress(comp.getCompanyId(), userName, password);
				} else {
					auth = ldap_auth_impl.authenticateByUserId(comp.getCompanyId(), userName, password);
				}

    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

    			try{
    				boolean SYNC_PASSWORD = BaseAuthenticator.SYNC_PASSWORD;
    				if(!SYNC_PASSWORD){
    					String roleName = LDAPImpl.LDAP_USER_ROLE;
    					if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, roleName)){
    						user.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);
    						APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
    					}
    				}
    			}catch (Exception e) {
    				Logger.debug(LoginFactory.class, "syncPassword not set or unable to load user", e);
    			}

    			match = auth == Authenticator.SUCCESS;
        	} else {
	            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

	            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
	            	throw new NoSuchUserException();
	            }

	            if (user.isNew() ||
	            		(!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive())) {
	            	  return false;
	            }

	            match = user.getPassword().equals(password) || user.getPassword().equals(PublicEncryptionFactory.digestString(password));

	            if (match) {
	            	user.setLastLoginDate(new java.util.Date());
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
	            } else {
	            	user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
	            }
        	}

            // if passwords match
            if (match) {
            	HttpSession ses = request.getSession();

                // session stuff
                ses.setAttribute(WebKeys.CMS_USER, user);

                //set personalization stuff on session

                // set id cookie
        		Cookie autoLoginCookie = UtilMethods.getCookie(request.getCookies(), WebKeys.CMS_USER_ID_COOKIE);

        		if(autoLoginCookie == null && rememberMe) {
        			autoLoginCookie = new Cookie(WebKeys.CMS_USER_ID_COOKIE, APILocator.getUserAPI().encryptUserId(user.getUserId()));
        		}

                if (rememberMe) {
                	autoLoginCookie.setMaxAge(60 * 60 * 24 * 356);
                } else if (autoLoginCookie != null) {
                	autoLoginCookie.setMaxAge(0);
                }

                if (autoLoginCookie != null) {
        			autoLoginCookie.setPath("/");
                	response.addCookie(autoLoginCookie);
                }

                return true;
            }
        } catch (NoSuchUserException e) {
        	throw e;
        } catch (Exception e) {
            Logger.error(LoginFactory.class, "Login Failed" + e);
        }

        return false;
    }

    /**
    *
    * @param userName
    * @param password
    * @param rememberMe
    * @param request
    * @param response
    * @return
    */
   public static boolean doLogin(String userName, String password) throws NoSuchUserException {
       try {
       	User user = null;
       	boolean match = false;
       	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

       	if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getEmailAddress())){
					return false;
				}
			} else {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getUserId())){
					return false;
				}
			}

       	if ((PRE_AUTHENTICATOR != null) &&
       		(0 < PRE_AUTHENTICATOR.length()) &&
       		PRE_AUTHENTICATOR.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"))) {
       		Class ldap_auth_impl_class = Class.forName(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"));
       		Authenticator ldap_auth_impl = (Authenticator) ldap_auth_impl_class.newInstance();
       		int auth = 0;

   			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
   				auth = ldap_auth_impl.authenticateByEmailAddress(comp.getCompanyId(), userName, password);
				} else {
					auth = ldap_auth_impl.authenticateByUserId(comp.getCompanyId(), userName, password);
				}

   			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

   			try{
   				boolean SYNC_PASSWORD = BaseAuthenticator.SYNC_PASSWORD;
   				if(!SYNC_PASSWORD){
   					String roleName = LDAPImpl.LDAP_USER_ROLE;
   					if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, roleName)){
   						user.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);
   						APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
   					}
   				}
   			}catch (Exception e) {
   				Logger.debug(LoginFactory.class, "syncPassword not set or unable to load user", e);
   			}

   			match = auth == Authenticator.SUCCESS;
       	} else {
	            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

	            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
	            	throw new NoSuchUserException();
	            }

	            if (user.isNew() ||
	            		(!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive())) {
	            	  return false;
	            }

	            match = user.getPassword().equals(password) || user.getPassword().equals(PublicEncryptionFactory.digestString(password));

	            if (match) {
	            	user.setLastLoginDate(new java.util.Date());
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
	            } else {
	            	user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
	            }
       	}

           // if passwords match
           if (match) {
               return true;
           }
       } catch (NoSuchUserException e) {
       	throw e;
       } catch (Exception e) {
           Logger.error(LoginFactory.class, "Login Failed" + e);
       }

       return false;
   }

    public static void doLogout(HttpServletRequest request, HttpServletResponse response) {

        //request.getSession().invalidate();
        /*
         * request.getSession().removeAttribute(WebKeys.SESSION_USER);
         * request.getSession().removeAttribute(com.liferay.portal.util.WebKeys.USER_ID);
         * request.getSession().removeAttribute(com.liferay.portal.util.WebKeys.USER);
         */

        request.getSession().removeAttribute("PENDING_ALERT_SEEN");
        request.getSession().removeAttribute("createAccountForm");
        request.getSession().removeAttribute("checkoutForm");
        request.getSession().removeAttribute(WebKeys.CMS_USER);
        request.getSession().removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
        request.getSession().removeAttribute(WebKeys.LOGGED_IN_USER_CATS);
        request.getSession().removeAttribute(WebKeys.LOGGED_IN_USER_TAGS);
        request.getSession().removeAttribute(WebKeys.USER_FAVORITES);

        Cookie idCookie = new Cookie(WebKeys.CMS_USER_ID_COOKIE, null);
        idCookie.setMaxAge(0);
        idCookie.setPath("/");
        response.addCookie(idCookie);

    }


}
