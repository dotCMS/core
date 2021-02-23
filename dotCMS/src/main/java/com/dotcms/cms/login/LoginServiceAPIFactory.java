package com.dotcms.cms.login;

import static com.dotmarketing.util.CookieUtil.createJsonWebTokenCookie;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.servlet.PortletSessionPool;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.InstancePool;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Login Service Factory that allows developers to inject custom login services.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public class LoginServiceAPIFactory implements Serializable {

     private static final String BACKEND_LOGIN = "backendLogin";

	/**
	 * Used to keep the instance of the {@link LoginServiceAPI}. Should be volatile
	 * to avoid thread-caching
	 */
    private volatile LoginServiceAPI loginService = null;

	/**
	 * Get the login service implementation from the
	 * dotmarketing-config.properties
	 */
    public static final String LOGIN_SERVICE_IMPLEMENTATION_KEY = "login.service.implementation";

    private LoginServiceAPIFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final LoginServiceAPIFactory INSTANCE = new LoginServiceAPIFactory();
    }

	/**
	 * Get the instance.
	 * 
	 * @return EncryptorFactory
	 */
    public static LoginServiceAPIFactory getInstance() {

        return LoginServiceAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Returns the custom Login Service, or the default implementation.
     * 
     * @return The {@link LoginServiceAPI}.
     */
    public LoginServiceAPI getLoginService () {

        String loginServiceFactoryClass = null;

        if (null == this.loginService) {

            synchronized (EncryptorFactory.class) {

                if (null == this.loginService) {

                    loginServiceFactoryClass =
                            Config.getStringProperty
                                    (LOGIN_SERVICE_IMPLEMENTATION_KEY, null);

                    if (UtilMethods.isSet(loginServiceFactoryClass)) {

                        if (Logger.isDebugEnabled(LoginServiceAPIFactory.class)) {

                            Logger.debug(LoginServiceAPIFactory.class,
                                    "Using the login service class: " + loginServiceFactoryClass);
                        }

                        this.loginService =
                                (LoginServiceAPI) ReflectionUtils.newInstance(loginServiceFactoryClass);

                        if (null == this.loginService) {

                            if (Logger.isDebugEnabled(LoginServiceAPIFactory.class)) {

                                Logger.debug(LoginServiceAPIFactory.class,
                                        "Could not used this class: " + loginServiceFactoryClass +
                                                ", using the default implementations");
                            }

                            this.loginService =
                                    new LoginServiceAPIFactory.LoginServiceImpl();
                        }
                    } else {

                        this.loginService =
                                new LoginServiceAPIFactory.LoginServiceImpl();
                    }
                }
            }
        }

        return this.loginService;
    }

    /**
     * Default implementation
     */
    private final class LoginServiceImpl implements LoginServiceAPI {

        private final Log log = LogFactory.getLog(LoginServiceAPI.class);
        private final UserWebAPI userWebAPI;
        private final JsonWebTokenUtils jsonWebTokenUtils;
        private final HttpServletRequestThreadLocal httpServletRequestThreadLocal;
        private final UserAPI userAPI;

        @VisibleForTesting
        public LoginServiceImpl(final ApiProvider apiProvider,
                                final JsonWebTokenUtils jsonWebTokenUtils,
                                final HttpServletRequestThreadLocal httpServletRequestThreadLocal,
                                final UserAPI userAPI){

            this.userWebAPI        = apiProvider.userWebAPI();
            this.jsonWebTokenUtils = jsonWebTokenUtils;
            this.httpServletRequestThreadLocal = httpServletRequestThreadLocal;
            this.userAPI = userAPI;
        }

        public LoginServiceImpl(){
            this(new ApiProvider(), JsonWebTokenUtils.getInstance(), HttpServletRequestThreadLocal.INSTANCE,
                    APILocator.getUserAPI());
        }

        @Override
        public void doActionLogout(final HttpServletRequest req,
                final HttpServletResponse res) throws Exception {

            final HttpSession session = req.getSession(false);
            if (null != session) {
                log.debug("Logout - Events Processor Pre Logout events.");
                EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGOUT_EVENTS_PRE), req, res);
            }

            log.debug("Logout - Set expire cookies");
            com.dotmarketing.util.CookieUtil.setExpireCookies(req, res);

            if (null != session) {
                final Map sessions = PortletSessionPool.remove(session.getId());
                if (null != sessions) {

                    log.debug("Logout - Invalidating portlet sessions...");

                    final Iterator itr = sessions.values().iterator();
                    while (itr.hasNext()) {

                        final HttpSession portletSession = (HttpSession) itr.next();
                        if (null != portletSession) {

                            portletSession.invalidate();
                        }
                    }
                }

                log.debug("Logout - Invalidating http session...");
                session.invalidate();

                log.debug("Logout - Events Processor Post Logout events.");
                EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGOUT_EVENTS_POST), req, res);
            }

        } // doActionLogout.

        @Override
        public boolean isLoggedIn(final HttpServletRequest req) {

            return LoginServiceAPI.super.isLoggedIn(req);
        }

        
        @CloseDBIfOpened
        @Override
        public boolean doBackEndLogin(String userId,
                                     final String password,
                                     final boolean rememberMe,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response) throws Exception {
            request.setAttribute(BACKEND_LOGIN,true);
            final boolean success = doActionLogin(userId, password,rememberMe,request,response);
            return success;
        
        }
        
        @CloseDBIfOpened
        @Override
        public boolean doActionLogin(String userId,
                                     final String password,
                                     final boolean rememberMe,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response) throws Exception {

            boolean authenticated = false;
            int authResult = Authenticator.FAILURE;

            final Company company = PortalUtil.getCompany(request);

            //Search for the system user
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            if ( Company.AUTH_TYPE_EA.equals(company.getAuthType()) ) {

                //Verify that the System User is not been use to log in inside the system
                if ( systemUser.getEmailAddress().equalsIgnoreCase( userId ) ) {
                    SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as a System User has been made  - you cannot login as the System User");
                    throw new AuthException( "Unable to login as System User - you cannot login as the System User." );
                }

                authResult = UserManagerUtil.authenticateByEmailAddress( company.getCompanyId(), userId, password );
                userId     = UserManagerUtil.getUserId( company.getCompanyId(), userId );
            } else {

                //Verify that the System User is not been use to log in inside the system
                if ( systemUser.getUserId().equalsIgnoreCase( userId ) ) {
                    SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as a System User has been made  - you cannot login as the System User");
                    throw new AuthException( "Unable to login as System User - you cannot login as the System User." );
                }

                authResult = UserManagerUtil.authenticateByUserId( company.getCompanyId(), userId, password );
            }

            try {

                final PrincipalFinder principalFinder =
                        (PrincipalFinder) InstancePool.get(
                                PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

                userId = principalFinder.fromLiferay(userId);
            }
            catch (Exception e) {

                // quiet
            }

            if (authResult == Authenticator.SUCCESS) {

                this.doAuthentication(userId, rememberMe, request, response);
                authenticated = true;
                LicenseUtil.licenseExpiresMessage(APILocator.getUserAPI().loadUserById(userId));
            }

            if (authResult != Authenticator.SUCCESS) {
                SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as " + userId + " has been made from IP: " + request.getRemoteAddr());
                throw new AuthException();
            }

            SecurityLogger.logInfo(this.getClass(), "User " + userId + " has successfully login from IP: " + request.getRemoteAddr());

            return authenticated;
        }

        @WrapInTransaction
        private void doAuthentication(final String userId, final boolean rememberMe,
                                      final HttpServletRequest  request,
                                      final HttpServletResponse response) throws PortalException, SystemException, DotDataException, DotSecurityException {

            final HttpSession session = request.getSession();
            final User user = UserLocalManagerUtil.getUserById(userId);

            if (null != user) {
                // User must be either back-end or front-end otherwise it must be rejected.
                if (!user.isFrontendUser() && !user.isBackendUser()) {
                    final String errorMessage = String
                            .format("User `%s` can not be identified neither as front-end nor back-end user ",
                                    user.getUserId());
                    SecurityLogger.logInfo(LoginServiceAPI.class, errorMessage);
                    throw new AuthException(errorMessage);
                }

                // if the authentication request comes from the backend user must have console access.
                final Object backendLogin = request.getAttribute(BACKEND_LOGIN);
                if (null != backendLogin && BooleanUtils.toBoolean(backendLogin.toString())) {
                    if (!user.hasConsoleAccess()) {
                        DateUtil.sleep(2000);
                        final String errorMessage = String
                                .format("User `%s` / `%s` login has failed. User does not have the Back End User Role or any layouts",
                                        user.getEmailAddress(), user.getUserId());
                        SecurityLogger.logInfo(this.getClass(), errorMessage);
                        //Technically this could be considered a SecurityException but in order for the error to be shown on the login screen we must throw an AuthException
                        throw new AuthException(errorMessage);
                    }
                }
            }

            //DOTCMS-4943
            final UserAPI userAPI = APILocator.getUserAPI();

            final Locale userSelectedLocale = LanguageUtil.getDefaultLocale(request);
            if (null != userSelectedLocale) {

                user.setLanguageId(userSelectedLocale.toString());
            }

            user.setLastLoginDate(new Date());
            user.setFailedLoginAttempts(0);
            user.setLastLoginIP(request.getRemoteAddr());
            userAPI.save(user, userAPI.getSystemUser(), true);

            session.setAttribute(WebKeys.USER_ID, userId);


            //set the host to the domain of the URL if possible if not use the default host
            //http://jira.dotmarketing.net/browse/DOTCMS-4475
            try{

                String domainName = request.getServerName();
                Host host = APILocator.getHostAPI().resolveHostName(domainName, user, false);

                if (null == host || !UtilMethods.isSet(host.getInode())) {
                    host = APILocator.getHostAPI().findByName(domainName, user, false);
                }

                if(host == null || !UtilMethods.isSet(host.getInode())){
                    host = APILocator.getHostAPI().findByAlias(domainName, user, false);
                }

                if(host != null && UtilMethods.isSet(host.getInode())) {
                    request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
                } else {
                    request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier());
                }
            } catch (DotSecurityException se) {

                request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier());
            }

            session.removeAttribute("_failedLoginName");

            this.preventSessionFixation(request);

            //JWT we crT always b/c in the future we want to use it not only for the remember me, but also for restful authentication.
            this.doRememberMe(request, response, user, rememberMe);

            EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_PRE), request, response);
            EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_POST), request, response);
        }

        /**
         * When an user is being logged in, the previous session must be invalidated and created a new one.
         * The default behavior does this, however it is able to turn off by using PREVENT_SESSION_FIXATION_ON_LOGIN on the dotmarketing config.
                * @param request HttpServletRequest
         * @return HttpSession return the new session in case it is created, otherwise returns the same one
         */
        @Override
        public HttpSession preventSessionFixation(final HttpServletRequest request) {

           return PreventSessionFixationUtil.getInstance().preventSessionFixation(request, false);
        } // preventSessionFixation.

        /**
         * Generates the JWT and its respective cookie based on the following
         * criteria:
         * <ul>
         * <li>Information from the user: ID, associated company.</li>
         * <li>The "Remember Me" option being checked or not.</li>
         * </ul>
         * The JWT will allow the user to access the dotCMS back-end even though
         * their session has expired. This way, they won't need to re-authenticate.
         *
         * @param req
         *            - The {@link HttpServletRequest} object.
         * @param res
         *            - The {@link HttpServletResponse} object.
         * @param user
         *            - The {@link User} trying to log into the system.
         * @param maxAge
         *            - The maximum days (in milliseconds) that the JWT will live.
         *            If the "Remember Me" option is checked, the token will live
         *            for many days (check its default value). Otherwise, it will
         *            only live during the current session.
         * @throws PortalException
         *             The specified user could not be found.
         * @throws SystemException
         *             An error occurred during the user ID encryption.
         */
        private void processJsonWebToken(final HttpServletRequest req,
                                         final HttpServletResponse res,
                                         final User user,
                                         final int maxAge) throws PortalException, SystemException {

            final String jwtAccessToken = this.jsonWebTokenUtils.createUserToken(user, Math.abs(maxAge));
            createJsonWebTokenCookie(req, res, jwtAccessToken, Optional.of(maxAge));
        }


        @Override
        @CloseDBIfOpened
        public boolean doLogin(final LoginForm form,
                               final HttpServletRequest request,
                               final HttpServletResponse response) throws NoSuchUserException {

            return LoginServiceAPI.super.doLogin(form, request, response);
        }

        @Override
        public void doRememberMe(final HttpServletRequest req,
                                 final HttpServletResponse res,
                                 final User user,
                                 final boolean rememberMe) {

            int jwtMaxAge = rememberMe ? Config.getIntProperty(
                    JSON_WEB_TOKEN_DAYS_MAX_AGE,
                    JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT) : -1;

            this.doRememberMe(req, res, user, jwtMaxAge);
        } // doRememberMe.

        @Override
        public void doRememberMe(final HttpServletRequest req,
                                 final HttpServletResponse res,
                                 final User user,
                                 final int maxAge) {

            try {

                this.processJsonWebToken(req, res, user, maxAge);
            } catch (Exception e) {

                Logger.debug(this, e.getMessage(), e);
            }
        } // doRememberMe.

        @CloseDBIfOpened
        @Override
        public boolean doCookieLogin(final String encryptedId, final HttpServletRequest request, final HttpServletResponse response) {
            // note: keep in mind we are doing BE and FE login, not sure if this is right
            final boolean doCookieLogin = LoginServiceAPI.super.doCookieLogin(encryptedId, request, response);

            if (doCookieLogin) {

                final String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
                request.setAttribute(WebKeys.USER_ID, decryptedId);
                final HttpSession session = request.getSession(false);
                if (null != session && null != decryptedId) {
                    // this is what the PortalRequestProcessor needs to check the login.
                    session.setAttribute(WebKeys.USER_ID, decryptedId);
                } //
            }

            return doCookieLogin;
        }

        @CloseDBIfOpened
        @Override
        public boolean doLogin(final String userName, final String password, final boolean rememberMe,
                               final HttpServletRequest request, final HttpServletResponse response) throws NoSuchUserException {

            return LoginServiceAPI.super.doLogin(userName, password, rememberMe, request, response);
        }

        @CloseDBIfOpened
        @Override
        public boolean doLogin(final String userName, final String password, final boolean rememberMe,
                               final HttpServletRequest request,
                               final HttpServletResponse response,
                               final boolean skipPasswordCheck) throws NoSuchUserException {

            return LoginServiceAPI.super.doLogin(userName, password, rememberMe, request, response, skipPasswordCheck);
        }

        @CloseDBIfOpened
        @Override
        public boolean doLogin(final String userName, final String password) throws NoSuchUserException {

            return LoginServiceAPI.super.doLogin(userName, password);
        }

        @Override
        public void doLogout(final HttpServletRequest request, final HttpServletResponse response) {

            LoginServiceAPI.super.doLogout(request, response);
        }

        @CloseDBIfOpened
        @Override
        public boolean passwordMatch(String password, User user) {
            return LoginFactory.passwordMatch(password, user);
        }

        /**
         * Return the current login user.
         * A {@link UserLoggingException} is thrown if any error happened getting the user.
         *
         * @param req
         * @return login user
         */
        public User getLoggedInUser(HttpServletRequest req ){
            User user = null;

            if (req != null) {
                user = userWebAPI.getLoggedInUser(req);
            }
            return user;
        }

        public User getLoggedInUser( ){
            HttpServletRequest request = httpServletRequestThreadLocal.getRequest();
            return this.getLoggedInUser(request);
        }
    }



} // E:O:F:LoginServiceAPIFactory.
