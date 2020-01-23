package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.ServiceDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

public class ServiceIntegrationView {

    private final long configurationsCount;

    private final String key;

    private final String name;

    private final String description;

    private final String iconUrl;

    @JsonInclude(Include.NON_NULL)
    private final List<SiteView> sites;

    public ServiceIntegrationView(final ServiceDescriptor serviceDescriptor, final long configurationsCount) {
        this.key = serviceDescriptor.getKey();
        this.name = serviceDescriptor.getName();
        this.description = serviceDescriptor.getDescription();
        this.iconUrl = serviceDescriptor.getIconUrl();
        this.configurationsCount = configurationsCount;
        this.sites = null;
    }

    public ServiceIntegrationView(final ServiceDescriptor serviceDescriptor, final long configurationsCount, final List<SiteView> sites) {
        this.key = serviceDescriptor.getKey();
        this.name = serviceDescriptor.getName();
        this.description = serviceDescriptor.getDescription();
        this.iconUrl = serviceDescriptor.getIconUrl();
        this.configurationsCount = configurationsCount;
        this.sites = sites;
    }

    public long getConfigurationsCount() {
        return configurationsCount;
    }

    public String getKey() {
        return key;
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

    public List<SiteView> getSites() {
        return sites;
    }
}
