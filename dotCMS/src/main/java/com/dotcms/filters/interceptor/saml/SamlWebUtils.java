package com.dotcms.filters.interceptor.saml;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import io.vavr.Lazy;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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


    public static final String BY_PASS_KEY   = "native";
    public static final Lazy<String> BY_PASS_VALUE = Lazy.of(()->Config.getStringProperty("SAML_BYPASS_VALUE","true"));
    public static final String AUTH_RELAYSTATE_KEY = "auth.relaystate";

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

        return BY_PASS_VALUE.get().equalsIgnoreCase(byPass);
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
    protected boolean isBackEndLoginPage(final String uri) {

        return uri.startsWith("/dotAdmin") || uri.startsWith("/html/portal/login") || uri.startsWith("/c/public/login")
                || uri.startsWith("/c/portal_public/login") || uri.startsWith("/c/portal/logout");
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
     * Return the relay state for the SAML request if it is possible
     * it could be a specific delegate class implemented or a generic one based on the velocity template indexed by the property auth.relaystate on the app SAML config
     * for instance this config: auth.relaystate=companyCode=request.getParameter('companyCode')
     * Will took the companyCode parameter from the request and will put it on the relay state
     * Otherwise null will be returned
     * @param request
     * @param response
     * @param identityProviderConfiguration
     * @param siteIdentifier
     * @return
     */
    public String getRelayState(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final IdentityProviderConfiguration identityProviderConfiguration,
                                final String siteIdentifier) {

        final String relayState = null != identityProviderConfiguration &&
                identityProviderConfiguration.containsOptionalProperty(AUTH_RELAYSTATE_KEY)?
                identityProviderConfiguration.getOptionalProperty(AUTH_RELAYSTATE_KEY).toString(): null;
        return getRelayState(request, response, identityProviderConfiguration, relayState, siteIdentifier);
    }

    /**
     * Based on the relayStateTemplate value will find a specific delegate class implemented or a generic one based on the velocity template
     * for instance if the value for the parameter relayStateTemplate is: companyCode=request.getParameter('companyCode')
     * Will took the companyCode parameter from the request and will put it on the relay state
     * Otherwise null will be returned
     * @param request
     * @param response
     * @param identityProviderConfiguration
     * @param relayStateTemplate
     * @param siteIdentifier
     * @return
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
