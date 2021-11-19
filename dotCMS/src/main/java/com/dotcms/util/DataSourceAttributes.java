
package com.dotcms.util;

import java.io.StringWriter;

public class DataSourceAttributes {
    public final String username;
    public final String url;
    private final char[] password;

    public DataSourceAttributes(final String username, final String password, final String url) {
        super();
        this.username = username;
        this.password = password.toCharArray();
        this.url = url;
    }

    @Override
    public String toString() {
        return "{username=" + username + ", password=******, url=" + url + "}";
    }

    public String getDbUrl() {
        StringWriter sw = new StringWriter();
        sw.append(url.substring(url.indexOf(":") + 1, url.indexOf("://") + 3))
        .append(username)
        .append(":")
        .append(new String(password))
        .append("@")
        .append(url.substring(url.indexOf("://") + 3, url.length()));
        return sw.toString();
    }
}
