
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
     * Filters out unsupported query parameters that pg_dump doesn't recognize.
     *
     * @return the db connection url.
     */
    public String getDbUrl() {
        String baseUrl = url.substring(url.indexOf(":") + 1, url.indexOf("://") + 3) +
                username +
                ":" +
                new String(password) +
                "@" +
                url.substring(url.indexOf("://") + 3);
        
        // Filter out unsupported query parameters for pg_dump
        // pg_dump only supports: host, port, dbname, user, password, sslmode, sslcert, sslkey, sslrootcert, sslcrl, requirepeer
        // Remove any other parameters to prevent "invalid URI query parameter" errors
        if (baseUrl.contains("?")) {
            String[] parts = baseUrl.split("\\?", 2);
            String connectionPart = parts[0];
            String queryPart = parts[1];
            
            // Parse and filter query parameters
            String[] params = queryPart.split("&");
            StringBuilder filteredParams = new StringBuilder();
            boolean hasValidParams = false;
            
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0].toLowerCase();
                
                // Only keep pg_dump supported parameters
                if (key.matches("^(host|port|dbname|user|password|sslmode|sslcert|sslkey|sslrootcert|sslcrl|requirepeer)$")) {
                    if (hasValidParams) {
                        filteredParams.append("&");
                    }
                    filteredParams.append(param);
                    hasValidParams = true;
                }
            }
            
            if (hasValidParams) {
                baseUrl = connectionPart + "?" + filteredParams.toString();
            } else {
                baseUrl = connectionPart;
            }
        }
        
        return baseUrl;
    }
}
