package com.dotcms.vanityurl.model;

import com.dotcms.vanityurl.handler.VanityUrlHandler;
import com.dotmarketing.filters.CMSFilter;

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
    private final CMSFilter.IAm iAm;
    private final String rewrite;
    private final boolean result;

    /**
     * Create a Vanity URL Object
     * @param rewrite The modified URL
     * @param queryString The query string of the modified url
     * @param iAm The type of URL (page, folder or file)
     * @param result If the rewrite was processed by the handler
     */
    public VanityUrlResult(final String rewrite, final String queryString, final CMSFilter.IAm iAm, final boolean result) {
        this.rewrite = rewrite;
        this.queryString = queryString;
        this.iAm = iAm;
        this.result = result;
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
     * Get the IAm enum value
     *
     * @return a IAM enum value
     */
    public CMSFilter.IAm getiAm() {
        return iAm;
    }

    /**
     * Get if this value was already processed
     * for the Handler
     *
     * @return true if was processed, false if not
     */
    public boolean isResult() {
        return result;
    }


}
