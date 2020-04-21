package com.dotcms.rest.api.v1.apps.view;

import com.dotcms.security.apps.AppDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

/**
 * Represents a service integration. Which serves as the top level entry for all the endpoints.
 * The view unfolds itself in the specifics for the associated sites.
 */
public class AppView {

    private final long configurationsCount;

    private final String key;

    private final String name;

    private final String description;

    private final String iconUrl;

    private final boolean allowExtraParams;

    @JsonInclude(Include.NON_NULL)
    private final List<SiteView> sites;

    /**
     * Used to build a site-less integration view
     * @param appDescriptor
     * @param configurationsCount
     */
    public AppView(final AppDescriptor appDescriptor, final long configurationsCount) {
        this.key = appDescriptor.getKey();
        this.name = appDescriptor.getName();
        this.description = appDescriptor.getDescription();
        this.iconUrl = appDescriptor.getIconUrl();
        this.allowExtraParams = appDescriptor.isAllowExtraParameters();
        this.configurationsCount = configurationsCount;
        this.sites = null;
    }

    /**
     * Use to build a more detailed integration view
     * Including site specific config info.
     * @param appDescriptor
     * @param configurationsCount
     * @param sites
     */
    public AppView(final AppDescriptor appDescriptor, final long configurationsCount, final List<SiteView> sites) {
        this.key = appDescriptor.getKey();
        this.name = appDescriptor.getName();
        this.description = appDescriptor.getDescription();
        this.iconUrl = appDescriptor.getIconUrl();
        this.allowExtraParams = appDescriptor.isAllowExtraParameters();
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
     * Whether or not extra params are supported
     * @return
     */
    public boolean isAllowExtraParams() {
        return allowExtraParams;
    }

    /**
     * All site specific configurations
     * @return
     */
    public List<SiteView> getSites() {
        return sites;
    }
}
