package com.dotcms.cms.login;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
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
import io.vavr.Lazy;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
import static com.dotmarketing.util.Constants.RESPECT_FRONT_END_ROLES;
import static com.dotmarketing.util.CookieUtil.createJsonWebTokenCookie;

/**
 * Login Service Factory that allows developers to inject custom login services.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public class LoginServiceAPIFactory implements Serializable {

    private static final Lazy<Boolean> FIRST_AVAILABLE_SITE_FALLBACK =
            Lazy.of(() -> Config.getBooleanProperty("FIRST_AVAILABLE_SITE_FALLBACK", true));
    private static final String BACKEND_LOGIN = "backendLogin";
    public static final String LOG_OUT_ATTRIBUTE = "LOG_OUT";

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
                session.setAttribute(LOG_OUT_ATTRIBUTE, true);
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

        /**
         * Executes the authentication process for the specified User ID. Several session-related
         * parameters are loaded as well. Additionally, users/developers can run their own Pre- and
         * Post-Login code, if necessary.
         *
         * @param userId     The ID of the user to authenticate.
         * @param rememberMe This parameter is no longer useful.
         * @param request    The current instance of the {@link HttpServletRequest}.
         * @param response   The current instance of the {@link HttpServletResponse}.
         *
         * @throws PortalException      Failed to retrieve the User matching the specified ID.
         * @throws SystemException      A system initialization error has occurred.
         * @throws DotDataException     An error occurred when interacting with the data source.
         * @throws DotSecurityException A permission problem when accessing the dotCMS APIs has
         *                              occurred.
         */
        @CloseDBIfOpened
        private void doAuthentication(final String userId, final boolean rememberMe,
                                      final HttpServletRequest  request,
                                      final HttpServletResponse response) throws PortalException, SystemException, DotDataException, DotSecurityException {
            final HttpSession session = request.getSession();
            final User user = UserLocalManagerUtil.getUserById(userId);

            if (null != user) {
                this.checkLoggedInUserAttributes(request, user);
            } else {
                throw new AuthException(String.format("User ID '%s' was not found", userId));
            }
            final Locale userSelectedLocale = LanguageUtil.getDefaultLocale(request);
            if (null != userSelectedLocale) {

                user.setLanguageId(userSelectedLocale.toString());
            }

            user.setLastLoginDate(new Date());
            user.setFailedLoginAttempts(0);
            user.setLastLoginIP(request.getRemoteAddr());
            userAPI.save(user, userAPI.getSystemUser(), RESPECT_FRONT_END_ROLES);
            session.setAttribute(WebKeys.USER_ID, userId);
            this.loadSiteIntoSession(request, user);

            session.removeAttribute("_failedLoginName");

            this.preventSessionFixation(request);

            //JWT we crT always b/c in the future we want to use it not only for the remember me, but also for restful authentication.
            this.doRememberMe(request, response, user, rememberMe);

            EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_PRE), request, response);
            EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_POST), request, response);
        }

        /**
         * Checks a specific set of attributes/characteristics that the User logging in must meet
         * for it to be correctly authenticated. For instance:
         * <ul>
         *     <li>It must be either a back-end or a front-end User</li>
         *     <li>If it's a back-end login, it must have access to <b>AT LEAST</b> one Portlet
         *     .</li>
         * </ul>
         *
         * @param request The current instance of the {@link HttpServletRequest}.
         * @param user    The {@link User} that is currently logged in.
         *
         * @throws AuthException If the User does not meet the required attributes.
         */
        private void checkLoggedInUserAttributes(final HttpServletRequest request, final User user) throws AuthException {
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
                    //Technically this could be considered a SecurityException but in order for the
                    // error to be shown on the login screen we must throw an AuthException
                    throw new AuthException(errorMessage);
                }
            }
        }

        /**
         * Resolves the Site that the User will be accessing now that it is logged in. There are
         * different ways of resolving the appropriate Site:
         * <ul>
         *     <li>Using the current domain name and look for a Site with a matching alias.</li>
         *     <li>Using the current domain name and look for a Site with that specific name.</li>
         *     <li>Returning the Default Site.</li>
         *     <li>Finally, look for the first Site that the User has READ permission to.</li>
         *     <li></li>
         * </ul>
         *
         * @param request The current instance of the {@link HttpServletRequest}.
         * @param user    The {@link User} that is currently logged in.
         *
         * @throws DotDataException     An error occurred when interacting with the data source.
         * @throws DotSecurityException A permission problem when accessing the dotCMS APIs has
         *                              occurred.
         * @throws AuthException        If the User does not have permission to any Site in the
         *                              repository, or the available site fallback mechanism is
         *                              disabled.
         */
        private void loadSiteIntoSession(final HttpServletRequest request, final User user) throws DotDataException, DotSecurityException, AuthException {
            final String domainName = request.getServerName();
            try {
                Host resolvedSite = APILocator.getHostAPI().resolveHostName(domainName, user, DONT_RESPECT_FRONT_END_ROLES);
                if (null == resolvedSite || !UtilMethods.isSet(resolvedSite.getInode())) {
                    resolvedSite = APILocator.getHostAPI().findByName(domainName, user, DONT_RESPECT_FRONT_END_ROLES);
                }
                if (resolvedSite == null || !UtilMethods.isSet(resolvedSite.getInode())) {
                    resolvedSite = APILocator.getHostAPI().findByAlias(domainName, user, DONT_RESPECT_FRONT_END_ROLES);
                }
                if (resolvedSite != null && UtilMethods.isSet(resolvedSite.getInode())) {
                    request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, resolvedSite.getIdentifier());
                } else {
                    request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), RESPECT_FRONT_END_ROLES).getIdentifier());
                }
            } catch (final DotSecurityException se) {
                Logger.warnAndDebug(LoginServiceAPIFactory.class, ExceptionUtil.getErrorMessage(se), se);
                this.handleAvailableSiteFallback(user, request);
            }
        }

        /**
         * Handles the available site fallback behavior when the logged-in User does not have
         * permission to access the selected Default Site. If the
         * {@code FIRST_AVAILABLE_SITE_FALLBACK} is enabled, Users can fall back to accessing the
         * first available Site that they have READ access to.
         *
         * @param loggedInUser The {@link User} that is currently logged in.
         * @param request      The current instance of the {@link HttpServletRequest}.
         *
         * @throws DotDataException     If an error occurred when interacting with the data source.
         * @throws DotSecurityException If a permission problem when accessing the dotCMS APIs has
         *                              occurred.
         * @throws AuthException        If the User does not have permission to any Site in the
         *                              repository, or the available site fallback mechanism is
         *                              disabled.
         */
        private void handleAvailableSiteFallback(final User loggedInUser,
                                                 final HttpServletRequest request) throws DotDataException, DotSecurityException, AuthException {
            final Optional<Host> defaultSiteOpt = this.findDefaultSite(loggedInUser);
            if (defaultSiteOpt.isPresent()) {
                Logger.warn(this, String.format("Setting the Default Site '%s' as current Site for User " +
                        "'%s'", defaultSiteOpt, loggedInUser.getUserId()));
                request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, defaultSiteOpt.get().getIdentifier());
            } else if (Boolean.TRUE.equals(FIRST_AVAILABLE_SITE_FALLBACK.get())) {
                final List<Host> availableSites = this.findAvailableSites(loggedInUser);
                if (!availableSites.isEmpty()) {
                    Logger.warn(this, String.format("User '%s' does not have READ permission to the Default Site. " +
                            "Setting the first available Site '%s' as current one", loggedInUser.getUserId(), availableSites.get(0)));
                    request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, availableSites.get(0).getIdentifier());
                } else {
                    Logger.error(this, String.format("User '%s' does not have permission to any Site in the repository. " +
                            "Please contact your CMS Administrator.", loggedInUser.getUserId()));
                }
            } else {
                Logger.error(this, String.format("User '%s' does not have permission to the Default Site. " +
                        "The FIRST_AVAILABLE_SITE_FALLBACK is disabled. User will not be able to access the system. " +
                        "Please contact your CMS Administrator.", loggedInUser.getUserId()));
                throw new AuthException(String.format("The User does not have permission to the current Default Site " +
                                "'%s'. Please contact your CMS Administrator.", defaultSiteOpt));
            }
        }

        /**
         * Finds the Default Site for the specified User.
         *
         * @param user The {@link User} to find the Default Site for.
         *
         * @return An {@link Optional} containing the Default Site if the User has permission to
         * access it. Otherwise, an Empty Optional is returned.
         *
         * @throws DotDataException If an error occurred when interacting with the data source.
         */
        private Optional<Host> findDefaultSite(final User user) throws DotDataException {
            try {
                final Host defaultSite = APILocator.getHostAPI().findDefaultHost(user, DONT_RESPECT_FRONT_END_ROLES);
                final boolean hasPermission =
                        APILocator.getPermissionAPI().doesUserHavePermission(defaultSite,
                                PermissionAPI.PERMISSION_READ, user, DONT_RESPECT_FRONT_END_ROLES);
                return hasPermission ? Optional.of(defaultSite) : Optional.empty();
            } catch (final DotSecurityException e) {
                Logger.debug(this, String.format("User '%s' does not have permission to retrieve the Default Site: %s",
                        user, ExceptionUtil.getErrorMessage(e)));
                return Optional.empty();
            }
        }

        /**
         * Finds all the available Sites that the specified User has READ permission to.
         *
         * @param user The {@link User} to find the available Sites for.
         *
         * @return A {@link List} of {@link Host} objects that the User has READ permission to.
         *
         * @throws DotDataException     If an error occurred when interacting with the data source.
         * @throws DotSecurityException If a permission problem when accessing the dotCMS APIs has
         *                              occurred.
         */
        private List<Host> findAvailableSites(final User user) throws DotDataException, DotSecurityException {
            final List<Host> availableSites = APILocator.getHostAPI().findAllFromCache(user, DONT_RESPECT_FRONT_END_ROLES);
            return availableSites.stream().filter(site -> {
                try {
                    return APILocator.getPermissionAPI()
                            .doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, DONT_RESPECT_FRONT_END_ROLES);
                } catch (final DotDataException e) {
                    Logger.debug(this, String.format("Failed to check READ permission of User " +
                            "'%s' for Site '%s': %s", user, site, ExceptionUtil.getErrorMessage(e)));
                    return false;
                }
            }).collect(Collectors.toList());
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
