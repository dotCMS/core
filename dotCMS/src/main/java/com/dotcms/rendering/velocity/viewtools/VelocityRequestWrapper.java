package com.dotcms.rendering.velocity.viewtools;

import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Xss;

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
    final protected static Set<String> SET_VALUE_BLACKLIST =
                    Config.getBooleanProperty("VELOCITY_PREVENT_SETTING_USER_ID", true)
                                    ? ImmutableSet.of(WebKeys.USER_ID, WebKeys.USER)
                                    : ImmutableSet.of();

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
        return new VelocitySessionWrapper(super.getSession());
    }

    @Override
    public HttpSession getSession(final boolean forceCreation) {
        HttpSession session = super.getSession(forceCreation);
        return session != null ? this.getSession() : null;
    }

    @Override
    public String getParameter(final String param) {
        String ret = super.getParameter(param);
        if (UtilMethods.isSet(ret) && Xss.URLHasXSS(ret)) {
            ret = UtilMethods.htmlifyString(ret);
        }
        return ret;
    }

    @Override
    public String getRealPath(final String path) {
        return path;
    }

    @Override
    public void removeAttribute(String key) {
        if (SET_VALUE_BLACKLIST.contains(key)) {
            return;
        }
        super.removeAttribute(key);
    }

    @Override
    public void setAttribute(final String key, final Object value) {
        if (SET_VALUE_BLACKLIST.contains(key)) {
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
