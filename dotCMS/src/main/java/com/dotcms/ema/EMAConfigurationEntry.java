package com.dotcms.ema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Provides the EMA configuration for any incoming URL that matches a given pattern via a RegEx. The JSON configuration
 * contains a list of different URL patterns, so this class maps each of them for easier access in the application.
 *
 * @author Jose Castro
 * @since May 12th, 2023
 */
public class EMAConfigurationEntry {

    private final String pattern;
    private final String urlEndpoint;
    private final boolean includeRendered;
    private final Map<String, Object> headers;

    /**
     * Creates a new EMA configuration for a given URL pattern.
     *
     * @param pattern         The RegEx that matches a specific URL pattern.
     * @param urlEndpoint     The URL to the third-party server that will handle the request.
     * @param includeRendered If the EMA service requires that dotCMS passes down the rendered version of the
     *                        incoming URL -- the HTML page --, set this to {@code true}.
     * @param headers         A Map with the list of HTTP Headers, if required.
     */
    @JsonCreator
    public EMAConfigurationEntry(@JsonProperty("pattern") String pattern,
                                 @JsonProperty("urlEndpoint") String urlEndpoint,
                                 @JsonProperty("includeRendered") boolean includeRendered,
                                 @JsonProperty("headers") Map<String, Object> headers) {
        this.pattern = pattern;
        this.urlEndpoint = urlEndpoint;
        this.includeRendered = includeRendered;
        this.headers = headers;
    }

    /**
     * Returns the RegEx pattern that matches the incoming URL.
     *
     * @return The RegEx pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns the URL to the third-party server that will handle the request.
     *
     * @return The URL to the third-party server.
     */
    public String getUrlEndpoint() {
        return urlEndpoint;
    }

    /**
     * Returns {@code true} if the EMA service requires that dotCMS passes down the rendered version of the incoming
     * URL, i.e., the HTML page.
     *
     * @return
     */
    public boolean isIncludeRendered() {
        return includeRendered;
    }

    /**
     * Returns a Map with the list of HTTP Headers.
     *
     * @return A Map with the list of HTTP Headers.
     */
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Returns the value of a specific HTTP Header.
     *
     * @param header The name of the HTTP Header.
     *
     * @return The value of the HTTP Header.
     */
    public String getHeader(final String header) {
        return null != this.headers && this.headers.containsKey(header) ? headers.get(header).toString() : null;
    }

    @Override
    public String toString() {
        return "EMAConfiguration{" + "pattern='" + pattern + '\'' + ", urlEndpoint='" + urlEndpoint + '\'' + ", " +
                       "includeRendered=" + includeRendered + ", headers=" + headers + '}';
    }

}
