package com.dotmarketing.business.web;

import com.dotcms.AppContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by freddyrodriguez on 31/8/16.
 */
public class WebContext implements AppContext {

    private HttpSession session;

    private WebContext(HttpServletRequest request){
        this.session = request.getSession();
    }

    public static WebContext getInstance(HttpServletRequest request){
        return new WebContext(request);
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

        WebContext that = (WebContext) o;

        return session != null ? session.equals(that.session) : that.session == null;

    }

    @Override
    public int hashCode() {
        return session != null ? session.hashCode() : 0;
    }
}
