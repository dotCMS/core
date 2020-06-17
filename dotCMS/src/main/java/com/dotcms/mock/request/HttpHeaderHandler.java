package com.dotcms.mock.request;

@FunctionalInterface
public interface HttpHeaderHandler {

    String getHeader(final String name, final String value);
}
