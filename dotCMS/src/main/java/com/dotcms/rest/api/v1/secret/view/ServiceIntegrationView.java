package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.ServiceDescriptor;

public class ServiceIntegrationView {

    private final long configurationsCount;

    private final String serviceKey;

    private final String name;

    private final String description;

    private final String iconUrl;

    public ServiceIntegrationView(final ServiceDescriptor serviceDescriptor, final long configurationsCount) {
        this.serviceKey = serviceDescriptor.getServiceKey();
        this.name = serviceDescriptor.getName();
        this.description = serviceDescriptor.getDescription();
        this.iconUrl = serviceDescriptor.getIconUrl();
        this.configurationsCount = configurationsCount;
    }

    public long getConfigurationsCount() {
        return configurationsCount;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }
}
