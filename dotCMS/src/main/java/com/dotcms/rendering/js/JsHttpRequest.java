package com.dotcms.rendering.js;

import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class JsHttpRequest extends HttpServletRequestWrapper {

    private final HttpServletRequest request;
    public JsHttpRequest(final HttpServletRequest request) {
        super(request);
        this.request = request;
    }

    @HostAccess.Export
    @Override
    public String getParameter(final String name) {
        return request.getParameter(name);
    }
}
