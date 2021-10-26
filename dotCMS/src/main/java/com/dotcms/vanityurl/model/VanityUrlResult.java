package com.dotcms.vanityurl.model;



/**
 * This class implements the Vanity URL response object
 * from the {@link VanityUrlHandler}
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 16, 2017
 */
public class VanityUrlResult {

    private final String queryString;
    private final String rewrite;
    private final boolean resolved;
    private final int responseCode;

    /**
     * Create a Vanity URL Object
     * @param rewrite The modified URL
     * @param queryString The query string of the modified url
     * @param resolved If the rewrite was processed by the handler
     */
    public VanityUrlResult(final String rewrite, final String queryString, final boolean resolved) {
        this.rewrite = rewrite;
        this.queryString = queryString;
        this.resolved = resolved;
        this.responseCode = 200;
    }

    /**
     * Create a Vanity Result that takes into account the responseCode of the vanity URL
     * @param rewrite
     * @param queryString
     * @param responseCode
     */
    public VanityUrlResult(final String rewrite, final String queryString, final int responseCode) {
        this.rewrite = rewrite;
        this.queryString = queryString;
        this.resolved = false;
        this.responseCode = responseCode;
    }

    /**
     * Get the url rewrite property
     *
     * @return a string with the rewrite property
     */
    public String getRewrite() {
        return rewrite;
    }

    /**
     * Get the url rewrite property queryString
     *
     * @return a string with the rewrite property queryString
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Get if this value was already processed
     * for the Handler
     *
     * @return true if was processed, false if not
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * VanityURL response Code
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }
}