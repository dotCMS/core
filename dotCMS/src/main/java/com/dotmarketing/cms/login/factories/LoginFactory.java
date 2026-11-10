package com.dotmarketing.cms.login.factories;

import com.dotcms.auth.LoginLockout;
import com.dotcms.cms.login.PreventSessionFixationUtil;
import com.dotcms.concurrent.lock.DotKeyLockManager;
import com.dotcms.concurrent.lock.DotKeyLockManagerBuilder;
import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.Role;
import com.google.common.annotations.VisibleForTesting;
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

	/**
	 * The authenticator named by {@code auth.pipeline.pre} as it stood when this class was loaded.
	 * Kept because it is part of this class's published surface; the login paths read the property
	 * through {@link #preAuthenticator()} instead, so a change takes effect without a restart.
	 */
	public static final String PRE_AUTHENTICATOR = PropsUtil.get(PropsUtil.AUTH_PIPELINE_PRE);


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
     * Signs a user in and puts them on the session, checking the password they typed.
     *
     * @param userName   the email address or user id, depending on the company's authentication type
     * @param password   the password typed by the user
     * @param rememberMe whether the login should be remembered beyond the session
     * @param request    the request the login was sent on
     * @param response   the response the session is established on
     * @return {@code true} when the user was signed in
     * @throws NoSuchUserException when dotCMS holds no account for the given name
     */
	public static boolean doLogin(String userName, String password, boolean rememberMe, HttpServletRequest request,
			HttpServletResponse response) throws NoSuchUserException {
		return doLogin(userName, password, rememberMe, request, response, false);
	}

	/**
	 * Signs a user in and puts them on the session.
	 *
	 * <p>The password stored in dotCMS is checked unless {@code skipPasswordCheck} is set, which is
	 * how a login that already proved the identity some other way, such as the cookie login, signs
	 * the user in. When the directory authenticator is configured as the pre-authenticator it has
	 * to accept the login as well; its answer alone is not taken as proof of the password, see
	 * {@link #directoryLoginAccepted(int, String, User, String)}.</p>
	 *
	 * @param userName          the email address or user id, depending on the company's authentication type
	 * @param password          the password typed by the user, ignored when the check is skipped
	 * @param rememberMe        whether the login should be remembered beyond the session
	 * @param request           the request the login was sent on
	 * @param response          the response the session is established on
	 * @param skipPasswordCheck when {@code true} the identity is taken as already proven and no
	 *                          password is checked
	 * @return {@code true} when the user was signed in
	 * @throws NoSuchUserException when dotCMS holds no account for the given name
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

        	if (directoryPreAuthenticatorConfigured()) {

				int auth = Authenticator.DNE;

				// if skipPasswordCheck is in true, means we do not have to check the user on LDAP just our DB
				if (!skipPasswordCheck) {

					auth = getLDAPAuth(userName, password, comp);
				}

    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }

				if (isRefusedAccount(user)) {
					SecurityLogger.logInfo(LoginFactory.class, "An invalid attempt to login to an inactive account as "
							+ userName + " from IP: " + request.getRemoteAddr());

					return false;
				}

				if (skipPasswordCheck) {

					// the user exists on our db and the login is ok (usually it is the case for cookie login),
					// and a login that skips the directory check leaves the local password alone
					match = null != user;
				} else {

					match = directoryLoginAccepted(auth, password, user, LoginLockout.clientAddress(request));
					if (!match && null != user && auth != Authenticator.SUCCESS) {
						// the directory refused the login; a wrong local password is counted on its own
						LoginLockout.getInstance().countFailure(user.getUserId(), LoginLockout.clientAddress(request));
					}

					// the directory stores the password it accepted in dotCMS, so the local password is
					// only replaced once it has been checked
					resetDirectoryPassword(user, auth);
				}
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
	            // Validate password and rehash when is needed. Refused while a lockout applies to this
	            // address or account; a failure counts toward the lock.
	            match = LoginLockout.getInstance().passwordMatch(password, user,
	            		LoginLockout.clientAddress(request), (secret, candidate) -> false);
	            }

	            if (match) {

	            	user.setLastLoginDate(new java.util.Date());
					user.setLastLoginIP(request.getRemoteAddr());
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);

	            } else {

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

	/**
	 * @return the authenticator configured as {@code auth.pipeline.pre}, read for every login the
	 * way {@code UserManagerImpl#authenticate} reads it, so the two paths agree on whether a
	 * pre-authenticator is in place
	 */
	@VisibleForTesting
	static String preAuthenticator() {

		return PropsUtil.get(PropsUtil.AUTH_PIPELINE_PRE);
	}

	/**
	 * @return {@code true} when the pre-authenticator in force is the directory authenticator this
	 * class knows how to call, which is the only case where the login is answered by a directory
	 */
	private static boolean directoryPreAuthenticatorConfigured() {

		final String preAuthenticator = preAuthenticator();
		return UtilMethods.isSet(preAuthenticator)
				&& preAuthenticator.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"));
	}

	/**
	 * Says whether an account must be refused whatever the credentials are, which is the check the
	 * login against dotCMS and {@code UserManagerImpl#checkUserStatus} both apply: an account that
	 * was never finished, or a deactivated one, unless {@code ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN}
	 * says otherwise.
	 *
	 * @param user the {@link User} the login names, {@code null} when dotCMS has no such account
	 * @return {@code true} when the login must be refused
	 */
	private static boolean isRefusedAccount(final User user) {

		return null != user && (user.isNew()
				|| (!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive()));
	}

	/**
	 * Decides a login that the directory service answered for.
	 *
	 * <p>The bundled directory authenticator answers {@link Authenticator#SUCCESS} both for a user
	 * it authenticated and for one it knows nothing about: {@code BaseAuthenticator} falls back to
	 * that answer for any account it did not find in the directory, where it only means "this is
	 * not mine to decide". The password stored in dotCMS is therefore still required, the way
	 * {@code UserManagerImpl#authenticate} requires it for the REST, Basic and WebDAV logins. A
	 * user the bundled authenticator did authenticate is not kept out by that check, because
	 * {@code BaseAuthenticator} stores the password it accepted in dotCMS before this runs. An
	 * authenticator that implements {@link Authenticator} without doing that has to keep the local
	 * password in step itself, as {@code UserManagerImpl#authenticate} already requires.</p>
	 *
	 * <p>The password check is the one that applies the login lockout, so a wrong password counts
	 * as a failed login and is refused while a lock is in force.</p>
	 *
	 * @param authResult the directory's answer, one of the {@link Authenticator} results
	 * @param password   the password typed by the user
	 * @param user       the {@link User} the login names, {@code null} when dotCMS has no such account
	 * @param source     what the attempt is counted under, usually the client address
	 * @return {@code true} when the login is accepted
	 */
	@VisibleForTesting
	static boolean directoryLoginAccepted(final int authResult, final String password, final User user,
			final String source) {

		if (authResult != Authenticator.SUCCESS || null == user) {
			return false;
		}

		return LoginLockout.getInstance().passwordMatch(password, user, source,
				(secret, candidate) -> false);
	}

	/**
	 * When directory passwords are not copied into dotCMS ({@code auth.impl.ldap.syncPassword}
	 * is off), replaces the local password of a directory user with the hash of a random
	 * secret, so the account can only sign in through the directory. Does nothing unless the
	 * directory accepted the login.
	 *
	 * @param user       the {@link User} that tried to sign in
	 * @param authResult the directory's answer, one of the {@link Authenticator} results
	 */
	@VisibleForTesting
	static void resetDirectoryPassword(final User user, final int authResult) {

		if (authResult != Authenticator.SUCCESS || BaseAuthenticator.SYNC_PASSWORD) {
			return;
		}

		try {

			final Role directoryRole = APILocator.getRoleAPI().loadRoleByKey(LDAPImpl.LDAP_USER_ROLE);
			if (APILocator.getRoleAPI().doesUserHaveRole(user, directoryRole)) {

				user.setPassword(PasswordFactoryProxy.generateRandomHash());
				user.setPasswordEncrypted(true);
				APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
			}
		} catch (Exception e) {

			Logger.debug(LoginFactory.class, "Unable to update the local password of a directory user", e);
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
    * Checks a user's credentials without creating a session. The password stored in dotCMS is
    * always checked; when the directory authenticator is configured as the pre-authenticator it
    * has to accept the login as well, and the local password of a directory user is only
    * replaced once that check has run.
    *
    * @param userName the email address or user id, depending on the company's authentication type
    * @param password the password typed by the user
    * @return {@code true} if the credentials are valid, {@code false} otherwise
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
        	if (directoryPreAuthenticatorConfigured()) {
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

    			if (isRefusedAccount(user)) {
 					SecurityLogger.logInfo(LoginFactory.class, "An invalid attempt to login as " + userName
 							+ " has been made  - user is marked inactive");

 					return false;
 				}

    			match = directoryLoginAccepted(auth, password, user, LoginLockout.clientAddress());
    			if (!match && null != user && auth != Authenticator.SUCCESS) {
    				// the directory refused the login; a wrong local password is counted on its own
    				LoginLockout.getInstance().countFailure(user.getUserId(), LoginLockout.clientAddress());
    			}

    			// the directory stores the password it accepted in dotCMS, so the local password is
    			// only replaced once it has been checked
    			resetDirectoryPassword(user, auth);
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

                // Refused while a lockout applies to this address or account; a failure counts toward the lock.
                match = LoginLockout.getInstance().passwordMatch(password, user);
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
     * Checks whether the password a user typed matches the one stored for that user. The typed
     * password is always hashed before it is compared: against the stored secure hash, or, for
     * values stored in the older digest format, against the digest of the typed password. A
     * stored value is never compared directly with the typed text. When an older format matches,
     * the stored value is upgraded to a secure hash and saved.
     *
     * <p>The value {@link DotCustomLoginPostAction#FAKE_PASSWORD} is reserved for accounts that
     * authenticate against a directory service and never matches.</p>
     *
     * @param password the password typed by the user
     * @param user     the {@link User}, loaded from the database with all its information,
     *                 because it is saved again when its stored password needs upgrading
     * @return {@code true} if the password matches, {@code false} otherwise
     */
    public static boolean passwordMatch(final String password, final User user) {
        if (DotCustomLoginPostAction.FAKE_PASSWORD.equals(password)) {
            return false;
        }

        if (!UtilMethods.isSet(user.getPassword())) {
            // Nothing to verify against. The hash verifier rejects a null or empty stored
            // value with an unchecked exception, which would surface as a server error
            // rather than as failed credentials.
            return false;
        }

        boolean needsToRehashPassword = false;

        try {
            if (PasswordFactoryProxy.isUnsecurePasswordHash(user.getPassword())) {
                // Older digest format: only the digest of the typed password can match it
                final boolean match = user.getPassword().equals(PublicEncryptionFactory.digestString(password));

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
