package com.dotcms.rest.api.v1.secret.view;

public class SiteView {

    private final String siteId;
    private final String siteName;

    public SiteView(final String siteId, final String siteName) {
        this.siteId = siteId;
        this.siteName = siteName;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getSiteName() {
        return siteName;
    }

}
