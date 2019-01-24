package com.dotcms.rendering.velocity.viewtools;

import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.google.common.collect.ImmutableSet;


public class VelocitySessionWrapper implements HttpSession {

    final private static Set<String> SET_VALUE_BLACKLIST = ImmutableSet.of("USER_ID");
    final private HttpSession session;

    public VelocitySessionWrapper(HttpSession session) {
        this.session = session;
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

    @Override
    public void removeAttribute(final String arg0) {
        session.removeAttribute(arg0);
    }

    @Override
    public void removeValue(final String arg0) {

        this.removeAttribute(arg0);

    }

    @Override
    public void setAttribute(final String arg0, final Object arg1) {
        if (!SET_VALUE_BLACKLIST.contains(arg0)) {
            session.setAttribute(arg0, arg1);
        }

    }

    @Override
    public void setMaxInactiveInterval(int arg0) {
        session.setMaxInactiveInterval(arg0);

    }


}
