
package com.dotcms.util;

import io.vavr.control.Try;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This class holds information fof the datasource connection to be used when exporting the DB as a postgres dump.
 *
 * @author vico
 */
public class DataSourceAttributes {
    public final String username;
    public final String url;
    private final char[] password;

    public DataSourceAttributes(final String username, final String password, final String url) {
        super();
        this.username = Try.of(()-> URLEncoder.encode(username, StandardCharsets.UTF_8.toString())).getOrNull();
        this.password = Try.of(()->URLEncoder.encode(password, StandardCharsets.UTF_8.toString()).toCharArray()).getOrElse(new char[]{});
        this.url = url;
    }

    @Override
    public String toString() {
        return "{username=" + username + ", password=******, url=" + url + "}";
    }

    /**
     * Builds a String representation of the DB connection url.
     *
     * @return the db connection url.
     */
    public String getDbUrl() {
        return url.substring(url.indexOf(":") + 1, url.indexOf("://") + 3) +
                username +
                ":" +
                new String(password) +
                "@" +
                url.substring(url.indexOf("://") + 3);
    }
}
