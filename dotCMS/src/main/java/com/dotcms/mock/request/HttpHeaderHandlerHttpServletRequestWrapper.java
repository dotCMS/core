package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

public class HttpHeaderHandlerHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, HttpHeaderHandler> httpHeaderHandlerMap = new HashMap<>();
    private final HttpHeaderHandler defaultHttpHeaderHandler = (name, value)-> value;

    public HttpHeaderHandlerHttpServletRequestWrapper(final HttpServletRequest request, final Map<String, HttpHeaderHandler> httpHeaderHandlerMap) {
        super(request);
        this.httpHeaderHandlerMap.putAll(httpHeaderHandlerMap);
    }

    @Override
    public String getHeader(final String name) {

        final String headerValue = super.getHeader(name);
        return this.httpHeaderHandlerMap.getOrDefault(name, this.defaultHttpHeaderHandler)
                .getHeader(name, headerValue);
    }
}
