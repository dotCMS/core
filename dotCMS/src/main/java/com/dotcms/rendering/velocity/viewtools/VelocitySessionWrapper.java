package com.dotcms.rendering.velocity.viewtools;

import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;


public class VelocitySessionWrapper implements HttpSession {


    final private HttpSession session;
    final private HttpServletRequest request;

    /**
     * Wraps a session for use from Velocity code, with no request to name in the log.
     *
     * @param session The session to wrap.
     */
    public VelocitySessionWrapper(HttpSession session) {
        this(session, null);
    }

    /**
     * Wraps a session for use from Velocity code.
     *
     * @param session The session to wrap.
     * @param request The request being rendered, or {@code null}; its URI is named when a call is refused.
     */
    public VelocitySessionWrapper(final HttpSession session, final HttpServletRequest request) {
        this.session = session;
        this.request = request;
    }

    /**
     * Tells whether Velocity code may set the given session attribute.
     *
     * @param key The attribute name.
     * @return {@code true} when the attribute is not in
     *         {@link VelocityRequestWrapper#NOT_SETTABLE_ATTRIBUTES}.
     */
    private boolean isSettable(final String key) {
        return VelocityRequestWrapper.isAllowed(VelocityRequestWrapper.NOT_SETTABLE_ATTRIBUTES, key,
                () -> session, this::requestUri);
    }

    /**
     * Tells whether Velocity code may remove the given session attribute. {@code USER_ID} and
     * {@code USER} can be removed, so a template can still end a visitor's session.
     *
     * @param key The attribute name.
     * @return {@code true} when the attribute is not in
     *         {@link VelocityRequestWrapper#NOT_REMOVABLE_SESSION_ATTRIBUTES}.
     */
    private boolean isRemovable(final String key) {
        return VelocityRequestWrapper.isAllowed(VelocityRequestWrapper.NOT_REMOVABLE_SESSION_ATTRIBUTES, key,
                () -> session, this::requestUri);
    }

    /**
     * Returns the URI of the request being rendered, for the log.
     *
     * @return The URI, or {@code null} when the wrapper has no request.
     */
    private String requestUri() {
        return null != request ? request.getRequestURI() : null;
    }

    @Override
    public Object getAttribute(final String arg0) {
        return session.getAttribute(arg0);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return session.getAttributeNames();
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getValue(final String arg0) {
        return session.getValue(arg0);
    }

    @Override
    public String[] getValueNames() {
        return session.getValueNames();
    }

    @Override
    public void invalidate() {
        session.invalidate();

    }

    @Override
    public boolean isNew() {
        return session.isNew();
    }

    @Override
    public void putValue(final String arg0, final Object arg1) {
        this.setAttribute(arg0, arg1);

    }

    /**
     * Removes the attribute from the session unless it is one that Velocity code may not remove.
     * {@code USER_ID} and {@code USER} can be removed from the session, though not from the request.
     *
     * @param arg0 The attribute name.
     */
    @Override
    public void removeAttribute(final String arg0) {
        if (isRemovable(arg0)) {
            session.removeAttribute(arg0);
        }
    }

    @Override
    public void removeValue(final String arg0) {

        this.removeAttribute(arg0);

    }

    /**
     * Sets the attribute on the session unless it is one that Velocity code may not set.
     *
     * @param arg0 The attribute name.
     * @param arg1 The attribute value.
     */
    @Override
    public void setAttribute(final String arg0, final Object arg1) {
        if (isSettable(arg0)) {
            session.setAttribute(arg0, arg1);
        }

    }

    @Override
    public void setMaxInactiveInterval(int arg0) {
        session.setMaxInactiveInterval(arg0);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((session == null) ? 0 : session.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VelocitySessionWrapper other = (VelocitySessionWrapper) obj;
        if (session == null) {
            if (other.session != null)
                return false;
        } else if (!session.equals(other.session))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "VelocitySessionWrapper [session=" + session + "]";
    }


}
