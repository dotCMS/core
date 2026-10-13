package com.dotcms.filters.interceptor.saml;

import com.dotcms.auth.dotAuth.DotAuthConstants;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates Saml util methods for Web.
 * @author jsanca
 */
public class SamlWebUtils {

    /**
     * In t case a client wants to override the default relay state strategy, it can be done by setting the by setting the addRelayStateStrategy
     * to the site id.
     */
    private static final Map<String, RelayStateStrategy> RELAY_STATE_STRATEGY_MAP = new ConcurrentHashMap<>();

    /*
    * This is the default implementation based on velocity evaluation
    */
    private static final RelayStateStrategy DEFAULT_VELOCITY_RELAY_STATE_STRATEGY = SamlWebUtils::evalRelayState;


    public static final String BY_PASS_KEY   = DotAuthConstants.BYPASS_PARAM;
    public static final String AUTH_RELAYSTATE_KEY = "auth.relaystate";

    /**
     * SAML app property that turns off the default RelayState (the visitor's requested page) that dotCMS sends
     * when no {@code auth.relaystate} template or {@link RelayStateStrategy} is configured. On unless set to
     * exactly {@code false} (case-insensitive).
     */
    public static final String AUTH_RELAYSTATE_DEFAULT_KEY = "auth.relaystate.default";

    /**
     * SAML app property listing extra hosts (comma-separated, exact host names) that a post-login redirect may
     * point to, besides the site's own hosts. A backward-compatibility valve for RelayState values that send users
     * to another host.
     */
    public static final String AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY = "auth.relaystate.allowed.hosts";

    /** Path prefixes of the dotCMS back-end login and logout pages. */
    private static final List<String> BACK_END_LOGIN_PREFIXES = List.of("/dotAdmin", "/html/portal/login",
            "/c/public/login", "/c/portal_public/login", "/c/portal/logout");

    protected     final UserWebAPI userWebAPI;

    public SamlWebUtils() {
        this(WebAPILocator.getUserWebAPI());
    }

    @VisibleForTesting
    public SamlWebUtils(final UserWebAPI userWebAPI) {
        this.userWebAPI = userWebAPI;
    }

    /**
     * Adds a custom relay state strategy for a site.
     * @param siteIdentifier
     * @param relayStateStrategy
     */
    public static void addRelayStateStrategy(final String siteIdentifier, final RelayStateStrategy relayStateStrategy) {
        RELAY_STATE_STRATEGY_MAP.put(siteIdentifier, relayStateStrategy);
    }

    protected boolean isByPass(final HttpServletRequest request, final HttpSession session) {

        String byPass = request.getParameter(BY_PASS_KEY);

        if (null != session) {
            if (null != byPass) {

                session.setAttribute(BY_PASS_KEY, byPass);
            } else {

                if (this.isNotLogged(request)) {

                    byPass = (String) session.getAttribute(BY_PASS_KEY);
                } else if (null != session.getAttribute(BY_PASS_KEY)) {

                    session.removeAttribute(BY_PASS_KEY);
                }
            }
        }

        return DotAuthConstants.getBypassValue().equalsIgnoreCase(byPass);
    }

    /**
     * Return true if the user is not logged. Work for FE and BE
     *
     * @param request
     *            {@link HttpServletRequest}
     * @return boolean
     */
    protected boolean isNotLogged(final HttpServletRequest request) {

        boolean isNotLogged     = true;
        final boolean isBackend = this.isBackEndAdmin(request, request.getRequestURI());

        try {

            isNotLogged = isBackend?
                    !this.userWebAPI.isLoggedToBackend(request):
                    null == this.userWebAPI.getLoggedInFrontendUser(request);

            Logger.debug(this, "Trying to go to back-end login? " + isBackend
                    + ", Is user NOT logged in? " + isNotLogged);
        } catch (PortalException | SystemException e) {

            Logger.error(this, e.getMessage(), e);
            isNotLogged = true;
        }

        return isNotLogged;
    }

    /**
     * Determines whether the user in the {@link HttpServletRequest} object or the incoming URI belong to the
     * dotCMS back-end login mechanism or not.
     *
     * @param request The {@link HttpServletRequest} request
     * @param uri     The incoming URI for login.
     *
     * @return If the user or its URI can be associated to the dotCMS back-end login, returns {@code true}. Otherwise,
     * returns {@code false}.
     */
    protected boolean isBackEndAdmin(final HttpServletRequest request, final String uri) {

        return PageMode.get(request).isAdmin || this.isBackEndLoginPage(uri);
    }

    /**
     * Analyzes the incoming URI and determines whether it belongs to dotCMS back-end login or logout URIs or not.
     *
     * @param uri The incoming URI.
     *
     * @return If the URI can be associated to the dotCMS back-end login or logout, returns {@code true}. Otherwise,
     * returns {@code false}.
     */
    public boolean isBackEndLoginPage(final String uri) {

        return BACK_END_LOGIN_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    /**
     * Same check as {@link #isBackEndLoginPage(String)}, ignoring letter case. Used by the post-login redirect
     * check, where a case-varied path such as {@code /DotAdmin/} must still get the back-end role check because
     * a proxy or CDN in front of dotCMS may normalize its case. The interceptor keeps the case-sensitive check,
     * so the sign-in flow a request gets is unchanged.
     *
     * @param uri The post-login destination path.
     *
     * @return {@code true} when the path starts with a back-end login or logout prefix in any letter case.
     */
    public boolean isBackEndLoginPageIgnoreCase(final String uri) {

        return BACK_END_LOGIN_PREFIXES.stream()
                .anyMatch(prefix -> uri.regionMatches(true, 0, prefix, 0, prefix.length()));
    }

    protected boolean isFrontEndLoginPage(final String uri){

        return uri.startsWith("/dotCMS/login") || uri.startsWith("/application/login");
    }

    public boolean isLogoutRequest(final String requestURI, final String... logoutPathArray) {

        Logger.debug(this, ()-> "----------------------------- isLogoutRequest --------------------------------");
        Logger.debug(this, ()-> "- requestURI = " + requestURI);
        Logger.debug(this, ()-> "- logoutPathArray = " + Arrays.asList(logoutPathArray));

        boolean isLogoutRequest = false;

        if (null != logoutPathArray) {
            for (final String logoutPath : logoutPathArray) {

                if (requestURI.startsWith(logoutPath) || requestURI.equals(logoutPath)) {

                    isLogoutRequest = true;
                    break;
                }
            }
        }

        Logger.debug(this, "- isLogoutRequest = " + isLogoutRequest);

        return isLogoutRequest;
    }

    protected HttpSession renewSession(final HttpServletRequest request, final HttpSession currentSession) {

        String attributeName                        = null;
        Object attributeValue                       = null;
        Enumeration<String> attributesNames         = null;
        HttpSession renewSession                    = currentSession;
        final Map<String, Object> sessionAttributes = new HashMap<>();

        if (null != currentSession && !currentSession.isNew()) {

            Logger.debug(this, ()-> "Renewing the HTTP session");

            attributesNames = currentSession.getAttributeNames();

            while (attributesNames.hasMoreElements()) {

                attributeName  = attributesNames.nextElement();
                attributeValue = currentSession.getAttribute(attributeName);
                Logger.debug(this, "Copying attribute '" + attributeName + "' to the new session.");
                sessionAttributes.put(attributeName, attributeValue);
            }

            Logger.debug(this, ()-> "Killing the current session");
            currentSession.invalidate(); // kill the previous session

            Logger.debug(this, ()-> "Creating a new one");
            renewSession = request.getSession(true);

            for (final Map.Entry<String, Object> sessionEntry : sessionAttributes.entrySet()) {

                Logger.debug(this, ()->"Adding attribute '" + sessionEntry.getKey() + "' to the new session.");
                renewSession.setAttribute(sessionEntry.getKey(), sessionEntry.getValue());
            }

        }

        return renewSession;
    }


    /**
     * Returns the RelayState to send to the IdP for this site, without a default: the value of the
     * {@link RelayStateStrategy} registered for the site, otherwise the evaluated {@code auth.relaystate}
     * template from the site's SAML configuration (e.g. {@code companyCode=$request.getParameter('companyCode')}),
     * otherwise null. The value is also stored in the session under {@link #AUTH_RELAYSTATE_KEY}.
     *
     * @param request                       the current request
     * @param response                      the current response
     * @param identityProviderConfiguration the site's SAML configuration
     * @param siteIdentifier                the site whose strategy, if any, is used
     * @return the RelayState, or null
     */
    public String getRelayState(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final IdentityProviderConfiguration identityProviderConfiguration,
                                final String siteIdentifier) {

        return getRelayState(request, response, identityProviderConfiguration,
                relayStateTemplate(identityProviderConfiguration), siteIdentifier);
    }

    /**
     * Returns the RelayState to send to the IdP for this site, without a default: the value of the
     * {@link RelayStateStrategy} registered for the site, otherwise the evaluated {@code relayStateTemplate}
     * (a Velocity template such as {@code companyCode=$request.getParameter('companyCode')}), otherwise null.
     * The value is also stored in the session under {@link #AUTH_RELAYSTATE_KEY}.
     *
     * @param request                       the current request
     * @param response                      the current response
     * @param identityProviderConfiguration the site's SAML configuration
     * @param relayStateTemplate            the template to evaluate when no strategy is registered
     * @param siteIdentifier                the site whose strategy, if any, is used
     * @return the RelayState, or null
     */
    public String getRelayState(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final IdentityProviderConfiguration identityProviderConfiguration,
                                final String relayStateTemplate,
                                final String siteIdentifier) {

        request.setAttribute(AUTH_RELAYSTATE_KEY, relayStateTemplate);

        final RelayStateStrategy relayStateStrategy = RELAY_STATE_STRATEGY_MAP.getOrDefault(siteIdentifier, DEFAULT_VELOCITY_RELAY_STATE_STRATEGY);

        final String relayStateValue =  relayStateStrategy.apply(request, response, identityProviderConfiguration);

        Optional.ofNullable(request.getSession()).ifPresent(session -> session.setAttribute(AUTH_RELAYSTATE_KEY, relayStateValue));

        return relayStateValue;
    }

    /**
     * Returns the RelayState to send to the IdP when a login starts, falling back to a default destination.
     * The order is: the {@link RelayStateStrategy} registered for the site (whatever it returns, even null),
     * then the site's {@code auth.relaystate} template, then {@code defaultRelayState}. A configured strategy
     * or template therefore produces exactly the same value as {@link #getRelayState}; the default is only
     * used when neither is configured. The value is also stored in the session under
     * {@link #AUTH_RELAYSTATE_KEY}.
     *
     * The caller is responsible for making {@code defaultRelayState} safe; the SAML interceptor passes the
     * visitor's destination only after it passes the same-host check.
     *
     * @param request                       the current request
     * @param response                      the current response
     * @param identityProviderConfiguration the site's SAML configuration
     * @param siteIdentifier                the site whose strategy, if any, is used
     * @param defaultRelayState             the value to send when no strategy or template is configured
     * @return the RelayState, or null
     */
    public String getRelayStateWithDefault(final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           final IdentityProviderConfiguration identityProviderConfiguration,
                                           final String siteIdentifier,
                                           final String defaultRelayState) {

        final String relayStateTemplate = relayStateTemplate(identityProviderConfiguration);
        if (RELAY_STATE_STRATEGY_MAP.containsKey(siteIdentifier) || UtilMethods.isSet(relayStateTemplate)) {
            return getRelayState(request, response, identityProviderConfiguration, relayStateTemplate, siteIdentifier);
        }

        Optional.ofNullable(request.getSession()).ifPresent(session -> session.setAttribute(AUTH_RELAYSTATE_KEY, defaultRelayState));
        return defaultRelayState;
    }

    /**
     * Returns the external hosts an admin explicitly allowed for post-login redirects, from the
     * {@link #AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY} property: comma-separated, trimmed, matched exactly
     * (case-insensitive). Entries containing a wildcard, scheme, port, path, user-info or whitespace are ignored,
     * so the list can only name exact hosts.
     *
     * @param identityProviderConfiguration the site's SAML configuration
     * @return the allowed external hosts; empty when none are configured
     */
    public static List<String> configuredAllowedHosts(final IdentityProviderConfiguration identityProviderConfiguration) {

        if (null == identityProviderConfiguration
                || !identityProviderConfiguration.containsOptionalProperty(AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)) {
            return Collections.emptyList();
        }
        final Object value = identityProviderConfiguration.getOptionalProperty(AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY);
        if (null == value || !UtilMethods.isSet(value.toString())) {
            return Collections.emptyList();
        }
        final List<String> hosts = new ArrayList<>();
        for (final String entry : value.toString().split(",")) {
            final String host = entry.trim();
            if (host.isEmpty()) {
                continue;
            }
            if (host.matches(".*[*/:@\\s].*")) {
                Logger.debug(SamlWebUtils.class, () -> "Ignoring an invalid entry in "
                        + AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY + ": only exact host names are allowed");
                continue;
            }
            hosts.add(host);
        }
        return hosts;
    }

    /**
     * Whether dotCMS may send its default RelayState (the visitor's requested page) for this SAML configuration.
     * It is on unless the {@link #AUTH_RELAYSTATE_DEFAULT_KEY} property is set to exactly {@code false}
     * (case-insensitive, surrounding spaces ignored); any other value keeps it on, so a typo cannot silently
     * change behavior. Turning it off restores the behavior from before the default existed.
     *
     * @param identityProviderConfiguration the site's SAML configuration
     * @return false only when the default RelayState is explicitly turned off
     */
    public static boolean isDefaultRelayStateEnabled(final IdentityProviderConfiguration identityProviderConfiguration) {

        if (null == identityProviderConfiguration
                || !identityProviderConfiguration.containsOptionalProperty(AUTH_RELAYSTATE_DEFAULT_KEY)) {
            return true;
        }
        final Object value = identityProviderConfiguration.getOptionalProperty(AUTH_RELAYSTATE_DEFAULT_KEY);
        return null == value || !"false".equalsIgnoreCase(value.toString().trim());
    }

    /**
     * Returns the hosts a post-login redirect may point to on this site: the host of the current request plus
     * the hostname and aliases of the site whose SAML configuration is in use. An IdP often sends a RelayState
     * on one alias of a site while its callback arrives on another, so all of them count as the same host.
     * Hosts of other sites are never included. If the site cannot be loaded, only the request host is returned.
     *
     * @param requestHost    the host of the current request ({@code request.getServerName()})
     * @param siteIdentifier the identifier of the site being logged in to (the SAML configuration id)
     * @return the allowed hosts, without duplicates
     */
    public static Collection<String> siteHosts(final String requestHost, final String siteIdentifier) {

        final Set<String> hosts = new LinkedHashSet<>();
        if (UtilMethods.isSet(requestHost)) {
            hosts.add(requestHost);
        }
        if (UtilMethods.isSet(siteIdentifier)) {
            try {
                final Host site = APILocator.getHostAPI().find(siteIdentifier, APILocator.systemUser(), false);
                if (null != site) {
                    if (UtilMethods.isSet(site.getHostname())) {
                        hosts.add(site.getHostname());
                    }
                    hosts.addAll(APILocator.getHostAPI().parseHostAliases(site));
                }
            } catch (final Exception e) {
                Logger.debug(SamlWebUtils.class, () -> "Unable to load the aliases of site " + siteIdentifier
                        + "; only the request host is allowed for post-login redirects: " + e.getMessage());
            }
        }
        return hosts;
    }

    /**
     * Removes the relay state strategy registered for a site, if any. Only used by tests, since the
     * registry is static and shared.
     *
     * @param siteIdentifier the site whose strategy is removed
     */
    @VisibleForTesting
    static void removeRelayStateStrategy(final String siteIdentifier) {
        RELAY_STATE_STRATEGY_MAP.remove(siteIdentifier);
    }

    /**
     * Returns the site's configured {@code auth.relaystate} Velocity template, or null when none is set.
     */
    private static String relayStateTemplate(final IdentityProviderConfiguration identityProviderConfiguration) {

        return null != identityProviderConfiguration &&
                identityProviderConfiguration.containsOptionalProperty(AUTH_RELAYSTATE_KEY)?
                identityProviderConfiguration.getOptionalProperty(AUTH_RELAYSTATE_KEY).toString(): null;
    }


    private static String evalRelayState(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final IdentityProviderConfiguration identityProviderConfiguration) {

        final String relayStateVelocityTemplate = (String) request.getAttribute(AUTH_RELAYSTATE_KEY);
        String velocityMessage        = relayStateVelocityTemplate;

        if (UtilMethods.isSet(relayStateVelocityTemplate)) {

            final Context velocityContext = VelocityUtil.getInstance().getContext(request, response);
            velocityContext.put("identityProviderConfiguration", identityProviderConfiguration);

            try {
                Logger.debug(SamlWebUtils.class, () -> "evaluating relay state template: " + relayStateVelocityTemplate);
                velocityMessage = VelocityUtil.eval(relayStateVelocityTemplate, velocityContext);
            } catch (Exception e1) {
                Logger.warn(SamlWebUtils.class, "unable to parse message, the relay state" + relayStateVelocityTemplate);
            }
        }

        return velocityMessage;
    }
} // E:O:F:SamlWebUtils.
