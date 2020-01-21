package com.dotcms.rest.api.v1.secret.view;

import java.util.List;

public class ServiceIntegrationSiteView {

    private final ServiceIntegrationView service;

    private final List<SiteView> sites;

    public ServiceIntegrationSiteView(
            final ServiceIntegrationView service,
            final List<SiteView> sites) {
        this.service = service;
        this.sites = sites;
    }

    public ServiceIntegrationView getService() {
        return service;
    }

    public List<SiteView> getSites() {
        return sites;
    }
}
