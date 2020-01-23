package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.Secret;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

public class SiteView {

    private final String siteId;
    private final String siteName;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, Secret> secrets;

    public SiteView(final String siteId, final String siteName) {
        this.siteId = siteId;
        this.siteName = siteName;
        this.secrets = null;
    }

    public SiteView(final String siteId,final String siteName,
            final Map<String, Secret> secrets) {
        this.siteId = siteId;
        this.siteName = siteName;
        this.secrets = secrets;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public Map<String, Secret> getSecrets() {
        return secrets;
    }
}
