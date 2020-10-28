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

}