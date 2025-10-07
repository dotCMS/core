package com.dotcms.rendering.velocity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;

/**
 * Emulates a recycled request
 * @author jsanca
 */
public class RecycledHttpServletRequest extends HttpServletRequestWrapper {

    public RecycledHttpServletRequest(final HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(final String name) {

        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }

    @Override
    public String getHeader(String name) {
        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }

    @Override
    public String getQueryString() {
        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }

    @Override
    public String getRequestURI() {
        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }

    @Override
    public HttpSession getSession() {
        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }

    @Override
    public Object getAttribute(String name) {
        throw new IllegalStateException("The request object has been recycled and is no longer associated with this facade");
    }
}
