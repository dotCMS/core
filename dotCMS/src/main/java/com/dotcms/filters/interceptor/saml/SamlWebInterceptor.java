package com.dotcms.filters.interceptor.saml;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.servlet.PortletSessionPool;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.saml.DotSamlConstants.SAML_USER_ID;

/**
 * This interceptor encapsulates the logic for Saml
 * Basically if there is any configuration set on dot apps portlet for "app-saml-config" and there is a configuration for the current host,
 * the interceptor will try to autologin the user (if there is any saml user id on the session) otherwise will redirect to the IDP login.
 * In addition if there is any saml host configuration for logout and the interceptor realized it is a logout request, will do the logout on dotCMS
 * in addition to the logout on the idp if the configuration requires.
 * @author jsanca
 */
public class SamlWebInterceptor implements WebInterceptor {

    public static final String REFERRER_PARAMETER_KEY = "referrer";
    public static final String ORIGINAL_REQUEST       = "original_request";

    protected final Encryptor       encryptor;
    protected final LoginServiceAPI loginService;
    protected final UserAPI         userAPI;
    protected final HostWebAPI      hostWebAPI;
    protected final AppsAPI         appsAPI;
    protected final SamlWebUtils    samlWebUtils;
    protected final CMSUrlUtil      cmsUrlUtil;
    protected final IdentityProviderConfigurationFactory identityProviderConfigurationFactory;
    protected volatile SamlConfigurationService samlConfigurationService;


    public SamlWebInterceptor() {

        this(EncryptorFactory.getInstance().getEncryptor(),
                APILocator.getLoginServiceAPI(),
                APILocator.getUserAPI(),
                WebAPILocator.getHostWebAPI(),
                APILocator.getAppsAPI(),
                new SamlWebUtils(),
                CMSUrlUtil.getInstance(),
                DotSamlProxyFactory.getInstance().identityProviderConfigurationFactory());
    }

    public SamlWebInterceptor(final Encryptor       encryptor,
            final LoginServiceAPI loginService,
            final UserAPI         userAPI,
            final HostWebAPI      hostWebAPI,
            final AppsAPI         appsAPI,
            final SamlWebUtils    samlWebUtils,
            final CMSUrlUtil      cmsUrlUtil,
            final IdentityProviderConfigurationFactory identityProviderConfigurationFactory) {

        this.encryptor    = encryptor;
        this.loginService = loginService;
        this.userAPI      = userAPI;
        this.hostWebAPI   = hostWebAPI;
        this.appsAPI      = appsAPI;
        this.samlWebUtils = samlWebUtils;
        this.cmsUrlUtil   = cmsUrlUtil;
        this.identityProviderConfigurationFactory = identityProviderConfigurationFactory;
    }

    private SamlConfigurationService samlConfig() {

        if (null == samlConfigurationService) {

            this.samlConfigurationService = Try.of(()->DotSamlProxyFactory.getInstance().samlConfigurationService()).getOrNull();
        }

        return this.samlConfigurationService;
    }

    @VisibleForTesting
    protected void setSamlConfig(final SamlConfigurationService samlConfigurationService) {

        this.samlConfigurationService = samlConfigurationService;
    }

    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (this.samlWebUtils.isByPass(request, session)) {

            Logger.info(this, ()->"Using SAML by pass");
            return Result.NEXT;
        }

        IdentityProviderConfiguration identityProviderConfiguration = null;

        try {

            if (null != this.samlConfig() && this.isAnySamlConfigurated()) {

                final Host host = hostWebAPI.getCurrentHostNoThrow(request);
                identityProviderConfiguration = // gets the SAML Configuration for this site.
                        null != host?
                                this.identityProviderConfigurationFactory.findIdentityProviderConfigurationById(host.getIdentifier()): null;

                // If idpConfig is null, means this site does not need SAML processing
                if (null != identityProviderConfiguration && identityProviderConfiguration.isEnabled()) { // SAML is configurated, so continue

                    // check if there is any exception filter path, to avoid to canApply all the logic.
                    if (!this.checkAccessFilters(request.getRequestURI(), host, request, this.getAccessFilterArray(identityProviderConfiguration))
                            && this.checkIncludePath(request.getRequestURI(), this.getIncludePathArray(identityProviderConfiguration))) {

                        if (this.samlWebUtils.isNotLogged(request)) {

                            final AutoLoginResult autoLoginResult = this.autoLogin(request, response,
                                    null != session? session: this.getSession(request), identityProviderConfiguration);

                            // we have to assign again the session, since the doAutoLogin might be renewed.
                            session = autoLoginResult.getSession();

                            // if the auto login couldn't logged the user, then send it to the IdP login page (if it is not already logged in).
                            if (null == session || !autoLoginResult.isAutoLogin() || this.samlWebUtils.isNotLogged(request)) {

                                this.doAuthentication(request, response, session, identityProviderConfiguration);
                                return Result.SKIP_NO_CHAIN;
                            }
                        }
                    }

                    final boolean isLogoutNeed = this.samlConfigurationService.getConfigAsBoolean(
                            identityProviderConfiguration, SamlName.DOTCMS_SAML_IS_LOGOUT_NEED);
                    // Starting the logout if it is logout
                    Logger.debug(this, ()-> "----------------------------- doFilter --------------------------------");
                    Logger.debug(this, ()-> "- isLogoutNeed = " + isLogoutNeed);
                    Logger.debug(this, ()-> "- httpServletRequest.getRequestURI() = " + request.getRequestURI());

                    if (isLogoutNeed &&
                            this.samlWebUtils.isLogoutRequest(request.getRequestURI(), this.getLogoutPathArray(identityProviderConfiguration))) {

                        if (this.doLogout(response, request, session, identityProviderConfiguration)) {

                            Logger.info(this, "SAML Logout DONE!!!");
                            return Result.SKIP_NO_CHAIN;
                        }
                    }
                } else {

                    Logger.debug(this, ()-> "No idpConfig for site '" + request.getServerName()
                            + "'. No SAML filtering for this request: " + request.getRequestURI());
                }
            }
        } catch (final Exception exception) {

            Logger.error(this,  "Error [" + exception.getMessage() + "] Unable to get idpConfig for Site '" +
                    request.getServerName() + "'. Incoming URL: " + request.getRequestURL(), exception);
            //response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            //return Result.SKIP_NO_CHAIN;
            // todo: do a better error handling
            return Result.NEXT;
        } finally {
            if (null != identityProviderConfiguration) {
                identityProviderConfiguration.destroy();
            }
        }

        return Result.NEXT;
    } // intercept.

    private HttpSession getSession (final HttpServletRequest httpServletRequest) {

        final HttpSession session = httpServletRequest.getSession(false);
        return session != null? session: httpServletRequest.getSession(true);
    }

    private void doAuthentication(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final HttpSession session,
                                  final IdentityProviderConfiguration identityProviderConfiguration) throws IOException {

        Logger.debug(this, ()-> "There's no logged-in user. Processing SAML request...");
        this.doRequestLoginSecurityLog(request, identityProviderConfiguration);

        final String originalRequest = request.getRequestURI() +
                (UtilMethods.isSet(request.getQueryString())?
                    "?" + request.getQueryString() : StringUtils.EMPTY);

        final String redirectAfterLogin = UtilMethods.isSet(request.getParameter(REFERRER_PARAMETER_KEY))
                ?request.getParameter(REFERRER_PARAMETER_KEY) :
                // this is safe, just to make a redirection when the user get's logged.
                originalRequest;

        Logger.debug(this.getClass(),
                ()-> "Executing SAML Login Redirection with request: " + redirectAfterLogin);

        // if we don't have a redirect yet
        if (null != session) {

            session.setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, redirectAfterLogin);
            session.setAttribute(ORIGINAL_REQUEST,             originalRequest);
        }

        try {
            // this will redirect the user to the IdP Login Page.
            DotSamlProxyFactory.getInstance().samlAuthenticationService()
                    .authentication(request, response, identityProviderConfiguration);
        } catch (Exception exception) {

            Logger.error(this,  "An error occurred when redirecting to the IdP Login page: " +
                    exception.getMessage(), exception);
            Logger.debug(this, ()-> "An error occurred when redirecting to the IdP Login page. Setting 500 " +
                    "response status.");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected boolean isAnySamlConfigurated() {

        return
                Try.of(()->this.appsAPI.getAppDescriptor(DotSamlProxyFactory.SAML_APP_CONFIG_KEY,
                        APILocator.systemUser())).getOrElseGet(e->Optional.empty()).isPresent();
    }

    protected AutoLoginResult autoLogin(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        HttpSession session,
                                        final IdentityProviderConfiguration identityProviderConfiguration) {

        final User user          = this.getUser(request, identityProviderConfiguration);
        boolean continueFilter   = null != user; // by default continue with the filter
        HttpSession renewSession = session;

        if (continueFilter) {
            // we are going to do the autologin, so if the session is null,
            // create it!
            try {

                Logger.debug(this, "User with ID '" + user.getUserId()
                        + "' has been returned by SAML Service. User " + "Map: " + user.toMap());
            } catch (Exception e) {

                Logger.error(this,
                        "An error occurred when retrieving data from user '" + user.getUserId() + "': " + e.getMessage(), e);
            }

            final boolean doCookieLogin = this.loginService
                    .doCookieLogin(this.encryptor.encryptString(user.getUserId()), request, response);

            Logger.debug(this, ()->"Cookie Login by LoginService = " + doCookieLogin);

            if (doCookieLogin) {

                session = request.getSession(false);
                if (null != session && null != user.getUserId()) {
                    // this is what the PortalRequestProcessor needs to check the login.
                    Logger.debug(this, ()->"Adding user ID '" + user.getUserId() + "' to the session");

                    final String uri = session.getAttribute(ORIGINAL_REQUEST) != null?
                            (String) session.getAttribute(ORIGINAL_REQUEST):
                            request.getRequestURI();

                    session.removeAttribute(ORIGINAL_REQUEST);

                    Logger.debug(this, ()->  "URI '" + uri + "' belongs to the back-end. Setting the user session data");
                    session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
                    session.setAttribute(com.liferay.portal.util.WebKeys.USER,    user);
                    PrincipalThreadLocal.setName(user.getUserId());

                    renewSession =
                            this.samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_RENEW_SESSION)?
                                this.samlWebUtils.renewSession(request, session): session;

                    this.doAuthenticationLoginSecurityLog(request, identityProviderConfiguration, user);
                }
            }
        }

        return new AutoLoginResult(renewSession, continueFilter);
    }

    public boolean doLogout(final HttpServletResponse response, final HttpServletRequest request,
                            final HttpSession session, final IdentityProviderConfiguration identityProviderConfiguration) {

        Logger.debug(this, ()-> "------------------------------ IdP doLogout ---------------------------------");

        final Object nameID           = null != session? session.getAttribute(identityProviderConfiguration.getId() + SamlAuthenticationService.SAML_NAME_ID) : null;
        final String samlSessionIndex = null != session? (String) session.getAttribute(identityProviderConfiguration.getId() + SamlAuthenticationService.SAML_SESSION_INDEX) : null;
        boolean doLogoutDone          = false;
        Logger.debug(this, ()-> "- idpConfig = " + identityProviderConfiguration);
        Logger.debug(this, ()-> "- NameID = " + nameID);
        Logger.debug(this, ()-> "- samlSessionIndex = " + samlSessionIndex);

        try {

            if (null != nameID && null != samlSessionIndex) {

                Logger.debug(this, ()-> "The URI '" + request.getRequestURI() + "' is a logout request. Executing the logout call to SAML");
                Logger.debug(this, ()-> "Executing dotCMS logout");

                doLogout(response, request);

                Logger.debug(this, ()-> "Executing SAML redirect logout");

                DotSamlProxyFactory.getInstance().samlAuthenticationService().logout(request, response, nameID, samlSessionIndex, identityProviderConfiguration);

                Logger.info(this, ()-> "User '" + nameID + "' has logged out");

                doLogoutDone = true;
            } else {

                Logger.warn(this, ()->"Couldn't execute the logout request. The SAML NameID or the SAML session index are not in the HTTP session");
            }
        } catch (Throwable e) {

            Logger.error(this, "Error on Logout: " + e.getMessage(), e);
            // todo: do something here???
        }

        Logger.debug(this, "- doLogoutDone = " + doLogoutDone);
        return doLogoutDone;
    }

    /**
     * Do the dotCMS logout
     *
     * @param response  {@link HttpServletResponse}
     * @param request  {@link HttpServletRequest}
     */
    protected void doLogout(final HttpServletResponse response, final HttpServletRequest request) { // todo: double check this to see if there is not anything else done on dotCMS core

        Logger.debug(this, ()-> "---------------------------- Generic doLogout -------------------------------");
        final Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (final Cookie cookie : cookies) {

                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }

        Logger.debug(this, ()-> "- Removing cookies...");
        final HttpSession session = request.getSession(false);
        Logger.debug(this, ()-> "- Invalidating session...");

        if (session != null) {

            Logger.debug(this, ()-> "- Session IS NOT null. Invalidating session maps...");
            final Map sessions = PortletSessionPool.remove(session.getId());

            if (sessions != null) {

                final Iterator itr = sessions.values().iterator();

                while (itr.hasNext()) {

                    final HttpSession portletSession = (HttpSession) itr.next();

                    if (portletSession != null) {

                        portletSession.invalidate();
                    }
                }
            }

            if (!session.isNew()) {

                Logger.debug(this, ()-> "- Logging out through the dotCMS Login Service...");
                this.loginService.doLogout(request, response);
            }
        }
    }

    public void doRequestLoginSecurityLog(final HttpServletRequest request,
                                          final IdentityProviderConfiguration identityProviderConfiguration) {

        try {

            final Host host  = this.hostWebAPI.getCurrentHost(request);
            final String env = this.samlWebUtils.isFrontEndLoginPage(request.getRequestURI()) ? "frontend" : "backend";
            final String log = new Date() + ": SAML login request for Site '" + host.getHostname() + "' with IdP ID: "
                    + identityProviderConfiguration.getId() + " (" + env + ") from " + request.getRemoteAddr();

            // “$TIMEDATE: SAML login request for $host (frontend|backend) from  $REQUEST_ADDR”
            SecurityLogger.logInfo(SecurityLogger.class, this.getClass()  + " - " + log);
            Logger.debug(this, log);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    protected void doAuthenticationLoginSecurityLog(final HttpServletRequest request,
                                                    final IdentityProviderConfiguration identityProviderConfiguration,
                                                    final User user) {
        try {

            final Host   host  = this.hostWebAPI.getCurrentHost(request);
            final String env   = this.samlWebUtils.isFrontEndLoginPage(request.getRequestURI()) ? "frontend" : "backend";
            final String log   = new Date() + ": Successfull SAML login for Site '" + host.getHostname() + "' with IdP " +
                    "ID: " + identityProviderConfiguration.getId() + " (" + env + ") from " + request.getRemoteAddr() + " for user: " +
                    user.getEmailAddress();

            // “$TIMEDATE: SAML login success for $host (frontend|backend) from $REQUEST_ADDR for user $username”
            SecurityLogger.logInfo(SecurityLogger.class, this.getClass() + " - " + log);
            Logger.info(this, log);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    /**
     * Perform the logic to get or create the user from the SAML and DotCMS If
     * the SAML_ART_PARAM_KEY, will resolve the Assertion by calling a Resolver
     * and will create/get/update the user on the dotcms data.
     *
     * @param request {@link HttpServletRequest}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return User
     */
    public User getUser(final HttpServletRequest request,
                        final IdentityProviderConfiguration identityProviderConfiguration) {

        User systemUser = null;
        User user       = null;

        final HttpSession session = request.getSession(false);
        if (session == null) {

            Logger.error(this, "Could not retrieve user from session as the session doesn't exist!");
            return null;
        }

        final String samlUserIdAttribute = identityProviderConfiguration.getId() + SAML_USER_ID;
        if (null == session.getAttribute(samlUserIdAttribute)) {
            return null;
        }

        final String samlUserId = (String) session.getAttribute(samlUserIdAttribute);
        session.removeAttribute(identityProviderConfiguration.getId() + SAML_USER_ID);

        try {
            systemUser = this.userAPI.getSystemUser();
            user       = this.userAPI.loadUserById(samlUserId, systemUser, false);
        } catch (NoSuchUserException e) {

            Logger.error(this, "No user matches ID '" + samlUserId + "'. Creating one...");
            user = null;
        } catch (Exception e) {

            Logger.error(this, "An error occurred when loading user with ID '" + samlUserId + "'", e);
            user = null;
        }

        return user;
    }

    /**
     * Get's the include urls to be analyzed by the open saml plugin, usually
     * the admin They can be a pattern
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String []
     */
    public String[] getIncludePathArray(final IdentityProviderConfiguration identityProviderConfiguration) {

        final String accessFilterValues = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
                SamlName.DOT_SAML_INCLUDE_PATH_VALUES);

        return UtilMethods.isSet(accessFilterValues)? accessFilterValues.split( "," ) : null;
    }

    /**
     * Returns the logout paths
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String[]
     */
    public String[] getLogoutPathArray(final IdentityProviderConfiguration identityProviderConfiguration) {

        final String logoutPathValues = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
                SamlName.DOT_SAML_LOGOUT_PATH_VALUES);

        return UtilMethods.isSet(logoutPathValues)? logoutPathValues.split( "," ) : null;
    }

    /**
     * Determine if the path is Backend Admin, usually it is for /c && /admin or
     * if the path is a file or path, will check if the user has permission
     *
     * @param uri
     *            {@link String}
     * @param includePaths
     *            {@link String} array
     * @return boolean
     */
    protected boolean checkIncludePath(final String uri, final String... includePaths) {

        boolean include = false;

        // this is the backend uri test.
        for (final String includePath : includePaths) {

            Logger.debug(this, ()-> "Evaluating URI '" + uri + "' with pattern: " + includePath);

            include |= RegEX.contains(uri, includePath);
        }

        Logger.debug(this, "Incoming URI '" + uri + "', include? " + include);

        return include;
    }

    /**
     * This method checks if some path does not wants to be treatment by the
     * {@link SamlWebInterceptor} An example of exception might be the destroy.jsp, so
     * on.
     *
     * @param uri
     *            {@link String}
     * @param filterPaths
     *            {@link String} array
     * @return boolean
     */
    protected boolean checkAccessFilters(final String uri, final Host host,
                                         final HttpServletRequest request, final String... filterPaths) {

        // filter it if it is an asset
        boolean filter = this.isFile(uri, host, WebAPILocator.getLanguageWebAPI().getLanguage(request).getId());

        if (null != filterPaths) {
            for (String filterPath : filterPaths) {
                filter |= uri.contains(filterPath);
            }
        }

        return filter;
    }

    private boolean isFile(final String uri, final Host host, final long languageId) {

        if (CMSFilter.IAm.FILE != this.cmsUrlUtil.resolveResourceType(null, uri, host, languageId)) {

            final String uriWithoutQueryString = this.cmsUrlUtil.getUriWithoutQueryString(uri.toLowerCase());
            return  uriWithoutQueryString.endsWith(".jpg")  ||
                    uriWithoutQueryString.endsWith(".jpeg") ||
                    uriWithoutQueryString.endsWith(".png")  ||
                    uriWithoutQueryString.endsWith(".gif")  ||
                    uriWithoutQueryString.endsWith(".css")  ||
                    uriWithoutQueryString.endsWith(".js")   ||
                    uriWithoutQueryString.endsWith(".js.map")         ||
                    uriWithoutQueryString.endsWith("manifest.json")   ||
                    uriWithoutQueryString.endsWith(".ttf");
        }

        return true;
    }

    public  String[] getAccessFilterArray(final IdentityProviderConfiguration identityProviderConfiguration) {

        final String accessFilterValues = this.samlConfigurationService.getConfigAsString(
                identityProviderConfiguration, SamlName.DOT_SAML_ACCESS_FILTER_VALUES);

        return UtilMethods.isSet(accessFilterValues)?
                accessFilterValues.split(",") : null;
    }

} // E:O:F:SamlWebInterceptor.
