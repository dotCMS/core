package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.Secret;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

/**
 * Represents the site and the secrets associated to it
 * Optionally The secrets can be null. In such case the view will only represent plain site info.
 */
public class SiteView {

    private final String id;
    private final String name;
    private final boolean configured;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, Secret> secrets;

    /**
     * Plain Secret-less Site view.
     * @param id
     * @param name
     */
    public SiteView(final String id, final String name) {
        this.id = id;
        this.name = name;
        this.configured = false;
        this.secrets = null;
    }

    /**
     * If we want to build a secret-less view but showing that the site has integrations.
     * @param id
     * @param name
     * @param configured
     */
    public SiteView(final String id, final String name, final boolean configured) {
        this.id = id;
        this.name = name;
        this.configured = configured;
        this.secrets = null;
    }

    /**
     * Plain Secret-detailed Site view.
     * @param id
     * @param name
     * @param secrets
     */
    public SiteView(final String id,final String name,
            final Map<String, Secret> secrets) {
        this.id = id;
        this.name = name;
        this.configured = null != secrets;
        this.secrets = secrets;
    }

    /**
     * site identifier
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * site name
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Shows secrets or not
     * @return
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Secrets per site
     * @return
     */
    public Map<String, Secret> getSecrets() {
        return secrets;
    }
}
