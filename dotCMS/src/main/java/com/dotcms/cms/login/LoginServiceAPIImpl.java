package com.dotcms.cms.login;

import static com.dotmarketing.util.CookieUtil.createJsonWebTokenCookie;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.events.LDAPLoginEventListener;
import com.dotcms.cms.login.events.LoginEvent;
import com.dotcms.cms.login.events.LoginEventListener;
import com.dotcms.cms.login.events.LoginEvents;
import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.cas.CASAuthUtils;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Validator;

/**
 * Default implementation
 */
final class LoginServiceAPIImpl implements LoginServiceAPI {


    private static final long serialVersionUID = 1L;
    private final Log log = LogFactory.getLog(LoginServiceAPI.class);
    private final UserWebAPI userWebAPI;
    private final JsonWebTokenUtils jsonWebTokenUtils;
    private final HttpServletRequestThreadLocal httpServletRequestThreadLocal;
    private final UserAPI userAPI;
    private final boolean authByEmail;
    private final User systemUser;
    private final String preAuthenticator;

    @VisibleForTesting
    public LoginServiceAPIImpl(final ApiProvider apiProvider,
                    final JsonWebTokenUtils jsonWebTokenUtils,
                    final HttpServletRequestThreadLocal httpServletRequestThreadLocal,
                    final UserAPI userAPI, boolean authByEmail, String preAuthenticator) {

        this.userWebAPI = apiProvider.userWebAPI();
        this.jsonWebTokenUtils = jsonWebTokenUtils;
        this.httpServletRequestThreadLocal = httpServletRequestThreadLocal;
        this.userAPI = userAPI;
        this.authByEmail = authByEmail;
        this.systemUser = APILocator.systemUser();
        this.preAuthenticator = preAuthenticator;
        this._listeners.add(new LDAPLoginEventListener());

    }

    @VisibleForTesting
    public LoginServiceAPIImpl(final ApiProvider apiProvider,
                    final JsonWebTokenUtils jsonWebTokenUtils,
                    final HttpServletRequestThreadLocal httpServletRequestThreadLocal,
                    final UserAPI userAPI) {
        this(apiProvider, jsonWebTokenUtils, httpServletRequestThreadLocal, userAPI,
                        (Company.AUTH_TYPE_EA).equals(
                                        PublicCompanyFactory.getDefaultCompany().getAuthType()),
                        PropsUtil.get("auth.pipeline.pre"));

    }

    public LoginServiceAPIImpl() {
        this(new ApiProvider(), JsonWebTokenUtils.getInstance(),
                        HttpServletRequestThreadLocal.INSTANCE, APILocator.getUserAPI());
    }



    @Override
    public void doLogout(final HttpServletRequest req, final HttpServletResponse res) {


        LoginEvent event = new LoginEvent(this, LoginEvents.PRE_LOGOUT, null, null, req, res);
        for (LoginEventListener listener : _listeners) {
            listener.loginEventReceived(event);
        }
        
        _doLogoutInternal(req, res);
        
        
        event = new LoginEvent(this, LoginEvents.POST_LOGOUT, null, null, req, res);
        for (LoginEventListener listener : _listeners) {
            listener.loginEventReceived(event);
        }
    }



    private void _doLogoutInternal(final HttpServletRequest req, final HttpServletResponse res) {

        final HttpSession session = req.getSession(false);

        log.debug("Logout - Set expire cookies");
        com.dotmarketing.util.CookieUtil.setExpireCookies(req, res);

        if (null != session) {

            log.debug("Logout - Events Processor Pre Logout events.");


            log.debug("Logout - Invalidating http session...");

            session.invalidate();

            log.debug("Logout - Events Processor Post Logout events.");

        }
    }



    /**
     * Generates the JWT and its respective cookie based on the following criteria:
     * <ul>
     * <li>Information from the user: ID, associated company.</li>
     * <li>The "Remember Me" option being checked or not.</li>
     * </ul>
     * The JWT will allow the user to access the dotCMS back-end even though their session has
     * expired. This way, they won't need to re-authenticate.
     *
     * @param req - The {@link HttpServletRequest} object.
     * @param res - The {@link HttpServletResponse} object.
     * @param user - The {@link User} trying to log into the system.
     * @param maxAge - The maximum days (in milliseconds) that the JWT will live. If the "Remember
     *        Me" option is checked, the token will live for many days (check its default value).
     *        Otherwise, it will only live during the current session.
     * @throws PortalException The specified user could not be found.
     * @throws SystemException An error occurred during the user ID encryption.
     */
    private void processJsonWebToken(final HttpServletRequest req, final HttpServletResponse res,
                    final User user, final int maxAge) throws PortalException, SystemException {

        final String jwtAccessToken = this.jsonWebTokenUtils.createToken(user, maxAge);
        createJsonWebTokenCookie(req, res, jwtAccessToken, Optional.of(maxAge));
    }



    @Override
    public void doRememberMe(final HttpServletRequest req, final HttpServletResponse res,
                    final User user, final boolean rememberMe) {

        int jwtMaxAge = rememberMe ? Config.getIntProperty(JSON_WEB_TOKEN_DAYS_MAX_AGE,
                        JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT) : -1;

        this.doRememberMe(req, res, user, jwtMaxAge);
    } // doRememberMe.

    @Override
    public void doRememberMe(final HttpServletRequest req, final HttpServletResponse res,
                    final User user, final int maxAge) {

        try {

            this.processJsonWebToken(req, res, user, maxAge);
        } catch (Exception e) {

            Logger.debug(this, e.getMessage(), e);
        }
    } // doRememberMe.


    private boolean _doCookieLogin(String encryptedId, HttpServletRequest request,
                    HttpServletResponse response) {

        try {
            String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
            /* Custom Code */
            User user = null;
            if (Validator.isEmailAddress(decryptedId))
                user = userAPI.loadByUserByEmail(decryptedId, systemUser, false);
            else
                user = userAPI.loadUserById(decryptedId, systemUser, false);
            /* End of Custom Code */
            try {
                String userName = (authByEmail) ? user.getEmailAddress() : user.getUserId();

                return _doLogin(userName, null, true, request, response, true);
            } catch (Exception e) { // $codepro.audit.disable logExceptions
                SecurityLogger.logInfo(this.getClass(),
                                "An invalid attempt to login (No user found) from IP: "
                                                + request.getRemoteAddr() + " :  " + e);

                return false;
            }
        } catch (Exception e) {
            SecurityLogger.logInfo(this.getClass(), "Auto login failed (No user found) from IP: "
                            + request.getRemoteAddr() + " :  " + e);

            doLogout(request, response);

            return false;

        }
    }



    @Override
    public boolean doCookieLogin(final String encryptedId, final HttpServletRequest request,
                    final HttpServletResponse response) {
        // note: keep in mind we are doing BE and FE login, not sure if this is right
        final boolean doCookieLogin = _doCookieLogin(encryptedId, request, response);



        return doCookieLogin;
    }

    @Override
    public boolean doLogin(final String userName, final String password, final boolean rememberMe,
                    final HttpServletRequest request, final HttpServletResponse response)
                    throws NoSuchUserException {

        return _doLogin(userName, password, rememberMe, request, response, false);
    }



    private boolean _doLogin(final String userIdOrEmail, final String password,
                    final boolean rememberMe, final HttpServletRequest request,
                    final HttpServletResponse response, final boolean skipPasswordCheck)
                    throws NoSuchUserException {

        LoginEvent event = new LoginEvent(this, LoginEvents.PRE_LOGIN, userIdOrEmail, password,
                        request, response);
        for (LoginEventListener listener : _listeners) {
            listener.loginEventReceived(event);
        }

        boolean auth = _doLoginInternal(userIdOrEmail, password, rememberMe, request, response,
                        false);

        event = (auth) ? new LoginEvent(this, LoginEvents.POST_LOGIN_SUCCESS, userIdOrEmail,
                        password, request, response)
                        : new LoginEvent(this, LoginEvents.POST_LOGIN_FAILURE, userIdOrEmail,
                                        password, request, response);
        for (LoginEventListener listener : _listeners) {
            listener.loginEventReceived(event);
        }

        return auth;


    }



    private boolean _doLoginInternal(final String userIdOrEmail, final String password,
                    final boolean rememberMe, final HttpServletRequest request,
                    final HttpServletResponse response, final boolean skipPasswordCheck)
                    throws NoSuchUserException {
        Company company = PublicCompanyFactory.getDefaultCompany();
        User user = null;

        try {



            // if skipPasswordCheck is in true, means we do not have to check the user on
            // LDAP just our DB
            if (!skipPasswordCheck) {
                if (runPreAuth(userIdOrEmail, password, company) == Authenticator.SUCCESS) {
                    syncPassword(_loadUserByKey(userIdOrEmail));
                }
            }

            user = _loadUserByKey(userIdOrEmail);

            if (!skipPasswordCheck) {
                passwordMatch(password, user);
            }



            doRememberMe(request, response, user, rememberMe);


            HttpSession ses = request.getSession();


            ses.setAttribute(com.dotmarketing.util.WebKeys.CMS_USER, user);
            ses.setAttribute(WebKeys.USER_ID, user.getUserId());
            SecurityLogger.logInfo(this.getClass(), "User " + userIdOrEmail
                            + " has sucessfully login from IP: " + request.getRemoteAddr());


            return true;

        } catch (NoSuchUserException e) {
            Logger.error(this.getClass(), "User " + userIdOrEmail + " does not exist.", e);
            SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as "
                            + userIdOrEmail + " has been made from IP: " + request.getRemoteAddr());
            throw e;

        } catch (Throwable e) {



            if (user != null) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                try {
                    APILocator.getUserAPI().save(user, systemUser, false);
                } catch (DuplicateUserException | DotDataException | DotSecurityException e1) {
                    Logger.error(this.getClass(), "Login Failed: " + e1);
                }
            }
            SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as "
                            + userIdOrEmail + " from IP: " + request.getRemoteAddr());


            Logger.error(this.getClass(), "Login Failed: " + e);
            SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as "
                            + userIdOrEmail + " has been made from IP: " + request.getRemoteAddr());
        }

        return false;
    }

    @Override
    public boolean doLogin(final String userName, final String password)
                    throws NoSuchUserException {

        HttpServletRequest request =
                        new MockSessionRequest(new MockHttpRequest("fake", "fake").request())
                                        .request();
        HttpServletResponse response =
                        new MockHttpResponse(new BaseResponse().response()).response();

        return _doLogin(userName, password, false, request, response, false);



    }

    @Override
    public boolean passwordMatch(String password, User user) throws DotSecurityException {


        try {
            boolean needsToRehashPassword = false;
            if (PasswordFactoryProxy.isUnsecurePasswordHash(user.getPassword())) {
                // This is the legacy functionality that we will removed with
                // the new hash library
                boolean match = user.getPassword().equals(password) || user.getPassword()
                                .equals(PublicEncryptionFactory.digestString(password));

                // Bad credentials
                if (!match) {
                    throw new DotSecurityException("passwords don't match");
                }

                // Force password rehash
                needsToRehashPassword = true;
            } else {
                PasswordFactoryProxy.AuthenticationStatus authStatus =
                                PasswordFactoryProxy.authPassword(password, user.getPassword());

                // Bad credentials
                if (authStatus.equals(
                                PasswordFactoryProxy.AuthenticationStatus.NOT_AUTHENTICATED)) {
                    throw new DotSecurityException("passwords don't match");
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
                APILocator.getUserAPI().save(user, systemUser, false);

                SecurityLogger.logInfo(this.getClass(),
                                "User password was rehash with id: " + user.getUserId());
            }
        } catch (PasswordException | DuplicateUserException | DotDataException
                        | DotSecurityException e) {
            Logger.error(this.getClass(),
                            "Error validating password from userId: " + user.getUserId(), e);
            throw new DotSecurityException("passwords don't match");
        }
        return true;
    }


    /**
     * Return the current login user. A {@link UserLoggingException} is thrown if any error happened
     * getting the user.
     *
     * @param req
     * @return login user
     */
    public User getLoggedInUser(HttpServletRequest req) {
        User user = null;

        if (req != null) {
            try {
                user = userWebAPI.getLoggedInUser(req);
            } catch (PortalException | SystemException e) {
                throw new UserLoggingException(e);
            }
        }
        return user;
    }

    public User getLoggedInUser() {
        HttpServletRequest request = this.httpServletRequestThreadLocal.getRequest();
        return this.getLoggedInUser(request);
    }


    private void syncPassword(User user) {
        try {

            final boolean SYNC_PASSWORD = BaseAuthenticator.SYNC_PASSWORD;
            if (!SYNC_PASSWORD) {

                String roleName = LDAPImpl.LDAP_USER_ROLE;

                if (APILocator.getRoleAPI().doesUserHaveRole(user, roleName)) {

                    user.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);
                    APILocator.getUserAPI().save(user, systemUser, false);
                }
            }
        } catch (Exception e) {

            Logger.debug(this.getClass(), "syncPassword not set or unable to load user", e);
        }
    }

    private int runPreAuth(final String userName, final String password, final Company comp)
                    throws AuthException {

        if (UtilMethods.isSet(preAuthenticator) && LicenseUtil.getLevel() > 100) {
            Authenticator authenticator =
                            (Authenticator) ReflectionUtils.newInstance(preAuthenticator);

            if (authByEmail) {
                return authenticator.authenticateByEmailAddress(comp.getCompanyId(), userName,
                                password);
            } else {
                return authenticator.authenticateByUserId(comp.getCompanyId(), userName, password);
            }

        }
        return Authenticator.DNE;

    }

    private User _loadUserByKey(String key)
                    throws DotDataException, DotSecurityException, NoSuchUserException {
        // Search for the system user

        User user = (authByEmail) ? userAPI.loadByUserByEmail(key, systemUser, false)
                        : userAPI.loadUserById(key, systemUser, false);

        if (systemUser.equals(user)) {
            throw new LoginSecurityException(
                            "An invalid attempt to login as a System User has been made  - you cannot login as the System User");
        }

        if (!user.isActive()) {
            throw new LoginSecurityException(
                            "User not active" + user.getUserId() + " " + user.getFullName());
        }
        if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
            throw new NoSuchUserException("No user found for with key" + key);
        }
        return user;


    }


    private List<LoginEventListener> _listeners = new CopyOnWriteArrayList<LoginEventListener>();


    public void addLoginEventListener(LoginEventListener listener) {
        _listeners.add(listener);

    }


    public void removeLoginEventListener(LoginEventListener listener) {
        _listeners.remove(listener);

    }

}
