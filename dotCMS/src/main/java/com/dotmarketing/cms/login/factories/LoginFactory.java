package com.dotmarketing.cms.login.factories;

import com.dotcms.cms.login.PreventSessionFixationUtil;
import com.dotcms.concurrent.lock.DotKeyLockManager;
import com.dotcms.concurrent.lock.DotKeyLockManagerBuilder;
import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Validator;

import io.vavr.control.Try;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * @author will
 *
 */
@Deprecated
public class LoginFactory {

	public static final String PRE_AUTHENTICATOR = PropsUtil.get("auth.pipeline.pre");


    private static final String LOCK_PREFIX = "UserIdLogin:";
    private static final DotKeyLockManager<String> lockManager = DotKeyLockManagerBuilder.newLockManager("LOGIN_LOCK");
    

    public static boolean doCookieLogin(String userId, HttpServletRequest request, HttpServletResponse response) {

        try {
            return lockManager.tryLock(LOCK_PREFIX + userId, () -> {

				// if multiple threads are stuck trying to login with the same encryptedId, we need to check if the user is already logged in
				if(PortalUtil.getUserId(request) != null) {
					return true;
				}
                String decryptedId = Try.of(()->PublicEncryptionFactory.decryptString(userId)).getOrElse(userId);
                if(decryptedId.equals(PortalUtil.getUserId(request))) {
                    return true;
                }
                
                /*Custom Code*/
				User user = null;
				if (Validator.isEmailAddress(decryptedId)) {
					user = APILocator.getUserAPI()
							.loadByUserByEmail(decryptedId, APILocator.getUserAPI().getSystemUser(), false);
				} else {
					user = APILocator.getUserAPI()
							.loadUserById(decryptedId, APILocator.getUserAPI().getSystemUser(), false);
				}

				if(user == null) {
					SecurityLogger.logInfo(LoginFactory.class,"Auto login failed (No user found) from  id: " + userId);
					return false;
				}

				if(!user.isActive()){
					SecurityLogger.logInfo(LoginFactory.class,"Auto login failed (User is not active) for user: "+ user.getEmailAddress() + " id: " +user.getUserId());
					return false;
				}


				final HttpSession session = PreventSessionFixationUtil.getInstance().preventSessionFixation(request, true);

				if(user.isBackendUser() || user.isAdmin()){
					session.setAttribute(WebKeys.CMS_USER, user);
					session.removeAttribute(com.dotmarketing.util.WebKeys.VISITOR);
					session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
					session.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
					request.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
					request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
					session.setAttribute(WebKeys.CMS_USER, user);
					SecurityLogger.logInfo(LoginFactory.class,"Successful login name:" + user.getFullName() + " id:+" + user.getUserId() +" email:" + user.getEmailAddress());
					session.setAttribute(WebKeys.CMS_USER, user);
					return true;
				}

				if(user.isFrontendUser()){
					SecurityLogger.logInfo(LoginFactory.class,"Successful front end login name:" + user.getFullName() + " id:+" + user.getUserId() +" email:" + user.getEmailAddress());
					session.setAttribute(WebKeys.CMS_USER, user);
					return true;
				}
				return false;

            });
        } catch (Throwable e) {
    		SecurityLogger.logInfo(LoginFactory.class,"Auto login failed (No user found) from : " + request.getRemoteAddr() + " :  " + e );

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
        		PRE_AUTHENTICATOR.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"))) {

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
					user.setLastLoginIP(request.getRemoteAddr());
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);

	            } else {

					match = false;
					user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
					APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " from IP: " + request.getRemoteAddr());

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
        		Authenticator ldap_auth_impl = (Authenticator) ldap_auth_impl_class.getDeclaredConstructor().newInstance();
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
    public static boolean passwordMatch(final String password, final User user) {
        boolean needsToRehashPassword = false;

        try {
            if (PasswordFactoryProxy.isUnsecurePasswordHash(user.getPassword())) {
                // This is the legacy functionality that we will replace with
                // the new hash library
                final boolean match = user.getPassword().equals(password)
                        || user.getPassword().equals(PublicEncryptionFactory.digestString(password));

                // Bad credentials
                if (!match) {
                    return false;
                }

                // Force password rehash
                needsToRehashPassword = true;
            } else {
                final PasswordFactoryProxy.AuthenticationStatus authStatus = PasswordFactoryProxy.authPassword(password,
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
            if (needsToRehashPassword) {
				// Apply new hash to the password and update user
                user.setPassword(PasswordFactoryProxy.generateHash(password));
                user.setLastLoginDate(new java.util.Date());
                APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
                SecurityLogger.logInfo(LoginFactory.class, String.format("User (%s) password was re-hashed 600000 times", user.getUserId()));
            }
        } catch (final PasswordException | DuplicateUserException | DotDataException | DotSecurityException e) {
            Logger.error(LoginFactory.class, "Error validating password from userId: " + user.getUserId(), e);
            return false;
        }

        return true;
    }

}
