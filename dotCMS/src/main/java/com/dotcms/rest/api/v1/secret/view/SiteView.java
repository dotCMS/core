package com.dotcms.rest.api.v1.secret.view;

public class SiteView {

    private String siteId;
    private String siteName;

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
