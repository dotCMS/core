package com.dotcms.rest.api.v1.secret.view;

import java.util.List;

public class ServiceIntegrationHostView {

    private final ServiceIntegrationView service;

    private final List<HostView> hosts;

    public ServiceIntegrationHostView(
            final ServiceIntegrationView service,
            final List<HostView> hosts) {
        this.service = service;
        this.hosts = hosts;
    }

    public ServiceIntegrationView getService() {
        return service;
    }

    public List<HostView> getHosts() {
        return hosts;
    }
}
