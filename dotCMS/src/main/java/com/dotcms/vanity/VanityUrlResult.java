package com.dotcms.vanity;

import com.dotmarketing.filters.CMSFilter;

/**
 * Created by oswaldogallango on 2017-06-16.
 */
public class VanityUrlResult {
    private final String queryString;
    private final CMSFilter.IAm iAm;
    private final String rewrite;

    public VanityUrlResult(String rewrite, String queryString, CMSFilter.IAm iAm, boolean result) {
        this.rewrite = rewrite;
        this.queryString = queryString;
        this.iAm = iAm;
        this.result = result;
    }

    public String getRewrite() {
        return rewrite;
    }

    public String getQueryString() {
        return queryString;
    }

    public CMSFilter.IAm getiAm() {
        return iAm;
    }

    public boolean isResult() {
        return result;
    }

    private final boolean result;
}
