package com.dotcms.rendering.velocity.viewtools;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotmarketing.business.LoginAsSession;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Xss;
import io.vavr.Lazy;

/**
 * This class wraps the incoming HttpServletRequest to provide security and to allow uris to be
 * overridden
 * 
 * @author will
 *
 */
public class VelocityRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {


    private String customUserAgentHeader;
    private String dotCMSUri;
    private static final boolean PREVENT_SETTING_USER_ID =
                    Config.getBooleanProperty("VELOCITY_PREVENT_SETTING_USER_ID", true);

    /** Attributes that Velocity code may not remove from the session. */
    static final Set<String> NOT_REMOVABLE_SESSION_ATTRIBUTES = PREVENT_SETTING_USER_ID
                    ? ImmutableSet.of(WebKeys.PRINCIPAL_USER_ID, LoginAsSession.SESSION_KEY,
                                    WebKeys.LOGIN_TO_EDIT_MODE, OAuthConstants.SESSION_NATIVE_LOGIN)
                    : ImmutableSet.of();

    /** Attributes that Velocity code may not set on the request or the session, nor remove from the request. */
    static final Set<String> NOT_SETTABLE_ATTRIBUTES = PREVENT_SETTING_USER_ID
                    ? ImmutableSet.<String>builder().add(WebKeys.USER_ID, WebKeys.USER)
                                    .addAll(NOT_REMOVABLE_SESSION_ATTRIBUTES).build()
                    : ImmutableSet.of();

    private static final Set<String> REFUSED_KEYS_LOGGED = ConcurrentHashMap.newKeySet();

    private static final Lazy<Boolean> USE_OWASP_ENCODING_FOR_XSS_PARAMS =
            Lazy.of(() -> Config.getBooleanProperty("USE_OWASP_ENCODING_FOR_XSS_PARAMS", true));

    /**
     * Tells whether Velocity code may set or remove the given request or session attribute. The
     * first refused call for each attribute is logged at WARN. The log is kept to once per
     * attribute, not per user, so the set of logged keys stays bounded by the size of the lists.
     *
     * @param refused The attributes the call may not change: {@link #NOT_SETTABLE_ATTRIBUTES} or
     *                {@link #NOT_REMOVABLE_SESSION_ATTRIBUTES}.
     * @param key     The attribute name.
     * @param session The session the call works on, or {@code null}; used to name the user in the log.
     * @param uri     The URI of the request being rendered, or {@code null}; used in the log.
     * @return {@code false} when the attribute is in {@code refused}.
     */
    static boolean isAllowed(final Set<String> refused, final String key, final Supplier<HttpSession> session,
                    final Supplier<String> uri) {
        if (!refused.contains(key)) {
            return true;
        }
        if (REFUSED_KEYS_LOGGED.add(key)) {
            final HttpSession currentSession = session.get();
            final Object userId = null != currentSession ? currentSession.getAttribute(WebKeys.USER_ID) : null;
            Logger.warn(VelocityRequestWrapper.class, String.format("Velocity code run by user '%s' for request " +
                    "'%s' tried to set or remove the attribute '%s', which only dotCMS sets; the call was ignored. " +
                    "This is logged once per attribute.", userId, uri.get(), key));
        }
        return false;
    }

    private VelocityRequestWrapper(final HttpServletRequest req) {
        super(req);

    }

    public String getActualParameter(final String param) {
        return super.getParameter(param);
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getHeader(final String header) {

        if ("user-agent".equalsIgnoreCase(header) && this.getCustomUserAgentHeader() != null) {
            return this.getCustomUserAgentHeader();
        }
        return super.getHeader(header);
    }


    @Override
    public String getRequestURI() {
        if (this.dotCMSUri != null) {
            return this.dotCMSUri;
        }
        return super.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(super.getScheme() + "://" + super.getServerName() + ":"
                        + super.getServerPort() + getRequestURI() + "?"
                        + UtilMethods.webifyString(super.getQueryString()));
    }

    @Override
    public HttpSession getSession() {
        return new VelocitySessionWrapper(super.getSession(), this);
    }

    @Override
    public HttpSession getSession(final boolean forceCreation) {
        HttpSession session = super.getSession(forceCreation);
        return session != null ? this.getSession() : null;
    }

    @Override
    public String getParameter(final String param) {
        String ret = super.getParameter(param);
        if (UtilMethods.isSet(ret) && Xss.urlHasXSS(ret)) {
            ret = USE_OWASP_ENCODING_FOR_XSS_PARAMS.get()
                    ? Xss.encodeForHTML(ret)
                    : UtilMethods.htmlifyString(ret);
        }
        return ret;
    }

    @Override
    public String getRealPath(final String path) {
        return path;
    }

    /**
     * Removes the request attribute unless it is one that Velocity code may not set. Unlike the
     * session, the request also refuses removal of {@code USER_ID} and {@code USER}, as it did before.
     *
     * @param key The attribute name.
     */
    @Override
    public void removeAttribute(String key) {
        if (!isAllowed(NOT_SETTABLE_ATTRIBUTES, key, () -> super.getSession(false), this::getRequestURI)) {
            return;
        }
        super.removeAttribute(key);
    }

    /**
     * Sets the request attribute unless it is one that Velocity code may not set.
     *
     * @param key   The attribute name.
     * @param value The attribute value.
     */
    @Override
    public void setAttribute(final String key, final Object value) {
        if (!isAllowed(NOT_SETTABLE_ATTRIBUTES, key, () -> super.getSession(false), this::getRequestURI)) {
            return;
        }
        
        super.setAttribute(key, value);
        
    }

    @Override
    public ServletContext getServletContext() {
        SecurityLogger.logInfo(this.getClass(), "User trying to access ServletContext from Velocity");
        return null;
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
        // do nothing
    }

    @Override
    public void logout() throws ServletException {
        // do nothing
    }

    public String getCustomUserAgentHeader() {
        return customUserAgentHeader;
    }

    public void setCustomUserAgentHeader(String customUserAgentHeader) {
        this.customUserAgentHeader = customUserAgentHeader;
    }

    public void setRequestUri(String uri) {
        this.dotCMSUri = uri;


    }

    public static VelocityRequestWrapper wrapVelocityRequest(final HttpServletRequest request) {
        if (request instanceof VelocityRequestWrapper) {
            return (VelocityRequestWrapper) request;
        }
        return new VelocityRequestWrapper(request);
    }


}
