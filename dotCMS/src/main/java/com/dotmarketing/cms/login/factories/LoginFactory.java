package com.dotmarketing.cms.login.factories;

import com.dotcms.cms.login.PreventSessionFixationUtil;
import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.cas.CASAuthUtils;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.*;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Validator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author will
 *
 */
@Deprecated
public class LoginFactory {

	public static String PRE_AUTHENTICATOR = PropsUtil.get("auth.pipeline.pre");

	/*Custom Code*/
	public static boolean useCASLoginFilter = new Boolean (Config.getBooleanProperty("FRONTEND_CAS_FILTER_ON",false));
	/*End of Custom Code*/

    public static boolean doLogin(LoginForm form, HttpServletRequest request, HttpServletResponse response) throws NoSuchUserException {
        return doLogin(form.getUserName(), form.getPassword(), form.isRememberMe(), request, response);
    }

    public static boolean doCookieLogin(String encryptedId, HttpServletRequest request, HttpServletResponse response) {

        try {
            String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
            /*Custom Code*/
            User user = null;
            if(Validator.isEmailAddress(decryptedId))
                user = APILocator.getUserAPI().loadByUserByEmail(decryptedId,APILocator.getUserAPI().getSystemUser(),false);
             else
                user = APILocator.getUserAPI().loadUserById(decryptedId,APILocator.getUserAPI().getSystemUser(),false);
            /* End of Custom Code */
            try {
                String userName = user.getEmailAddress();
                Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
                if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
                	userName = user.getUserId();
                }
                return doLogin(userName, null, true, request, response, true);
            } catch (Exception e) { // $codepro.audit.disable logExceptions
        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login (No user found) from IP: " + request.getRemoteAddr() + " :  " + e );

            	return false;
            }
        } catch (Exception e) {
    		SecurityLogger.logInfo(LoginFactory.class,"Auto login failed (No user found) from IP: " + request.getRemoteAddr() + " :  " + e );

            if(useCASLoginFilter){
            	String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
            	Logger.info(LoginFactory.class, "Try to retrieve user from LDAP/CAS with id: " + decryptedId);
            	User newUser = CASAuthUtils.loadUserFromLDAP(decryptedId);
            	
            	if(UtilMethods.isSet(newUser)){
            		User user = null;
            		Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
            		try {
						if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
							user = APILocator.getUserAPI().loadByUserByEmail(decryptedId, APILocator.getUserAPI().getSystemUser(), false);
						} else {
							user = APILocator.getUserAPI().loadUserById(decryptedId, APILocator.getUserAPI().getSystemUser(), false);
						}
                        
	            		String userIdFromCAS = (String)request.getSession(false).getAttribute("edu.yale.its.tp.cas.client.filter.user");
	            		
						if(UtilMethods.isSet(userIdFromCAS)){
							CASAuthUtils.setUserValuesOnSession(user, request, response, true);
						}

                        return true;
                         
                    } catch (Exception ex) {
                    	return false;
                    }
            	}
            	else
            		Logger.info(LoginFactory.class, "Unable to retrieve user from LDAP/CAS with id: " + decryptedId);
            }

        doLogout(request, response);

        return false;

        }
    }

    /**
     *
     * @param userName
     * @param password
     * @param rememberMe
     * @param request
     * @param response
     * @return
     * @throws NoSuchUserException
     */
	public static boolean doLogin(String userName, String password, boolean rememberMe, HttpServletRequest request,
			HttpServletResponse response) throws NoSuchUserException {
		return doLogin(userName, password, rememberMe, request, response, false);
	}

	/**
	 * 
	 * @param userName
	 * @param password
	 * @param rememberMe
	 * @param request
	 * @param response
	 * @param skipPasswordCheck
	 * @return
	 * @throws NoSuchUserException
	 */
    public static boolean doLogin(String userName, String password, boolean rememberMe, HttpServletRequest request, HttpServletResponse response, boolean skipPasswordCheck) throws NoSuchUserException {
        try {
        	User user = null;
        	boolean match = false;
        	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

        	if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getEmailAddress())){
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login with email as " + userName + " from IP: " + request.getRemoteAddr());

					return false;
				}
			} else {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getUserId())){
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login with userID as " + userName + " from IP: " + request.getRemoteAddr());

					return false;
				}
			}

        	if ((PRE_AUTHENTICATOR != null) &&
        		(0 < PRE_AUTHENTICATOR.length()) &&
        		PRE_AUTHENTICATOR.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION")) &&
        		!useCASLoginFilter) {

				int auth = 0;

				// if skipPasswordCheck is in true, means we do not have to check the user on LDAP just our DB
				if (!skipPasswordCheck) {

					auth = getLDAPAuth(userName, password, comp);
				}

    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

				// if we do not get the user from LDAP, does not make sense to sync the password.
				if (!skipPasswordCheck) {

					syncPassword(user);
				}

				// if it is the skip password on true and the user is not null, means the user exists on our db and the login is ok (usually it is the case for cookie login)
				if (skipPasswordCheck && null != user) {

					auth = Authenticator.SUCCESS;
				}

    			match = auth == Authenticator.SUCCESS;
        	} else {
	            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

	            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login or no email set for " + userName + " from IP: " + request.getRemoteAddr());

	            	throw new NoSuchUserException();
	            }

	            if (user.isNew() ||
	            		(!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive())) {
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login to an inactive account as " + userName + " from IP: " + request.getRemoteAddr());

	            	return false;
	            }
	            match = true;
	            if (!skipPasswordCheck) {
	            // Validate password and rehash when is needed
	            match = passwordMatch(password, user);
	            }

	            if (match) {

	            	user.setLastLoginDate(new java.util.Date());
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);

	            } else {
	            	/*Custom code*/
					if(useCASLoginFilter){
	            		
	            		String userIdFromCAS = (String)request.getSession(false).getAttribute("edu.yale.its.tp.cas.client.filter.user");
	            		
						if(UtilMethods.isSet(userIdFromCAS)){
							user = CASAuthUtils.syncExistingUser(user);
							match=true;
						}
	            	}

	            	/* end of custom code*/
	            	else{
	            		match = false;
		            	user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
		            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
		        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " from IP: " + request.getRemoteAddr());
	            	}
	            }
        	}

            // if passwords match
            if (match) {
                //finally we need to verify if they user can be identified as a front-end or back-end user.
				if (null != user) {
					if (!user.isFrontendUser() && !user.isBackendUser()) {
						SecurityLogger.logInfo(LoginFactory.class, String.format("User `%s` can not be identified neither as front-end nor back-end user ", user.getUserId()));
						return false;
					}
				}

            	final HttpSession session = PreventSessionFixationUtil.getInstance().preventSessionFixation(request, true);
            	session.removeAttribute(com.dotmarketing.util.WebKeys.VISITOR);
                // session stuff
                session.setAttribute(WebKeys.CMS_USER, user);

                //set personalization stuff on session
				// ....

        		SecurityLogger.logInfo(LoginFactory.class,"User " + userName + " has sucessfully logged in from IP: " + request.getRemoteAddr());

                return true;
            }
        } catch (NoSuchUserException e) {
            Logger.error(LoginFactory.class, "User " + userName + " does not exist.", e);
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made from IP: " + request.getRemoteAddr());
        	throw e;
        } catch (Exception e) {
            Logger.error(LoginFactory.class, "Login Failed: " + e, e);
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made from IP: " + request.getRemoteAddr());
        }

        return false;
    }

	private static void syncPassword(User user) {
		try {

            final boolean SYNC_PASSWORD = BaseAuthenticator.SYNC_PASSWORD;
            if (!SYNC_PASSWORD) {

                String roleName = LDAPImpl.LDAP_USER_ROLE;

                if (APILocator.getRoleAPI().doesUserHaveRole(user, roleName)) {

                    user.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);
                    APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
                }
            }
        } catch (Exception e) {

            Logger.debug(LoginFactory.class, "syncPassword not set or unable to load user", e);
        }
	}

	private static int getLDAPAuth(final String userName,
								   final String password,
								   final Company comp) throws ClassNotFoundException, InstantiationException, IllegalAccessException, AuthException {

		int auth = Authenticator.DNE;
		final Class ldap_auth_impl_class = Class.forName
                (Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"));
		final Authenticator ldap_auth_impl = (Authenticator) ldap_auth_impl_class.newInstance();

		if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
            auth = ldap_auth_impl.authenticateByEmailAddress(comp.getCompanyId(), userName, password);
        } else {
            auth = ldap_auth_impl.authenticateByUserId(comp.getCompanyId(), userName, password);
        }
		return auth;
	}


	/**
    *
    * @param userName
    * @param password
    * @return
    */
    public static boolean doLogin(String userName, String password) throws NoSuchUserException {
        try {
        	User user = null;
        	boolean match = false;
        	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

        	if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
 				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getEmailAddress())){
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - you cannot login as the system user");

 					return false;
 				}
 			} else {
 				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getUserId())){
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - you cannot login as the system user");

 					return false;
 				}
 			}

        	// LDAP Authentication
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
        	    // Liferay authentication
 	            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
 	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
 	            } else {
 	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
 	            }

 	            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - user cannot be found");

 	            	throw new NoSuchUserException();
 	            }

 	            if (user.isNew() ||
 	            		(!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive())) {
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - user is marked inactive");

 	            	return false;
 	            }

                match = passwordMatch(password, user);
                if (match == false) {
                    user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                    APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
                }
        	}

            // if passwords match
            if (match) {
                return true;
            }
        } catch (NoSuchUserException e) {
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made :" + e);

        	throw e;
        } catch (Exception e) {
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made :" + e);

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
        request.getSession().removeAttribute(WebKeys.VISITOR);
        request.getSession().invalidate();
    }

    /**
     * This method validates legacy, clear and new passwords. When identifies a
     * clear or legacy password it will validate and then change it to a
     * stronger hash then save it into the system.
     * 
     * @param password
     *            string entered by the user
     * @param user
     *            MUST be loaded from db with all the information because we are
     *            going to save this object if rehash process required
     * @return
     */
    public static boolean passwordMatch(final String password, User user) {
        boolean needsToRehashPassword = false;

        try {
            if (PasswordFactoryProxy.isUnsecurePasswordHash(user.getPassword())) {
                // This is the legacy functionality that we will removed with
                // the new hash library
                boolean match = user.getPassword().equals(password)
                        || user.getPassword().equals(PublicEncryptionFactory.digestString(password));

                // Bad credentials
                if (!match) {
                    return false;
                }

                // Force password rehash
                needsToRehashPassword = true;
            } else {
                PasswordFactoryProxy.AuthenticationStatus authStatus = PasswordFactoryProxy.authPassword(password,
                        user.getPassword());

                // Bad credentials
                if (authStatus.equals(PasswordFactoryProxy.AuthenticationStatus.NOT_AUTHENTICATED)) {
                    return false;
                }

                // Force password rehash
                if (authStatus.equals(PasswordFactoryProxy.AuthenticationStatus.NEEDS_REHASH)) {
                    needsToRehashPassword = true;
                }
            }

            // Apply new hash to the password and update user
            if (needsToRehashPassword) {
                // We need to rehash password and save the new ones
                user.setPassword(PasswordFactoryProxy.generateHash(password));
                user.setLastLoginDate(new java.util.Date());
                APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);

                SecurityLogger.logInfo(LoginFactory.class, "User password was rehash with id: " + user.getUserId());
            }
        } catch (PasswordException | DuplicateUserException | DotDataException | DotSecurityException e) {
            Logger.error(LoginFactory.class, "Error validating password from userId: " + user.getUserId(), e);
            return false;
        }

        return true;
    }

}
