package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface HttpHeaderHandler {

    String getHeader(final String name, final String value);
}
