package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.ServiceDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

/**
 * Represents a service integration. Which serves as the top level entry for all the endpoints.
 * The view unfolds itself in the specifics for the associated sites.
 */
public class ServiceIntegrationView {

    private final long configurationsCount;

    private final String key;

    private final String name;

    private final String description;

    private final String iconUrl;

    @JsonInclude(Include.NON_NULL)
    private final List<SiteView> sites;

    /**
     * Used to build a site-less integration view
     * @param serviceDescriptor
     * @param configurationsCount
     */
    public ServiceIntegrationView(final ServiceDescriptor serviceDescriptor, final long configurationsCount) {
        this.key = serviceDescriptor.getKey();
        this.name = serviceDescriptor.getName();
        this.description = serviceDescriptor.getDescription();
        this.iconUrl = serviceDescriptor.getIconUrl();
        this.configurationsCount = configurationsCount;
        this.sites = null;
    }

    /**
     * Use to build a more detailed integration view
     * Including site specific config info.
     * @param serviceDescriptor
     * @param configurationsCount
     * @param sites
     */
    public ServiceIntegrationView(final ServiceDescriptor serviceDescriptor, final long configurationsCount, final List<SiteView> sites) {
        this.key = serviceDescriptor.getKey();
        this.name = serviceDescriptor.getName();
        this.description = serviceDescriptor.getDescription();
        this.iconUrl = serviceDescriptor.getIconUrl();
        this.configurationsCount = configurationsCount;
        this.sites = sites;
    }

    /**
     * number of configuration (Total count)
     * @return
     */
    public long getConfigurationsCount() {
        return configurationsCount;
    }

    /**
     * Service unique identifier
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * any given name
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Any given description
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * The url of the avatar used on the UI
     * @return
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * All site specific configurations
     * @return
     */
    public List<SiteView> getSites() {
        return sites;
    }
}
