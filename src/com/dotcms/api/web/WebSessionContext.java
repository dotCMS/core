package com.dotcms.api.web;

import com.dotcms.system.AppContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Wrapper to HttpSession
 */
public final class WebSessionContext implements AppContext {

    private HttpSession session;

    private WebSessionContext(HttpServletRequest request){
        this.session = request.getSession();
    }

    public static WebSessionContext getInstance(HttpServletRequest request){
        return new WebSessionContext(request);
    }

    @Override
    public <T> T getAttribute(String attributeName) {
        return (T) this.session.getAttribute(attributeName);
    }

    @Override
    public <T> void setAttribute(String attributeName, T attributeValue) {
        this.session.setAttribute(attributeName, attributeValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebSessionContext that = (WebSessionContext) o;

        return session != null ? session.equals(that.session) : that.session == null;

    }

    @Override
    public int hashCode() {
        return session != null ? session.hashCode() : 0;
    }
}
