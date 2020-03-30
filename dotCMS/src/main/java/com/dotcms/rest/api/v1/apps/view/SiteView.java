package com.dotcms.rest.api.v1.apps.view;

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
    private final Map<String, SecretView> secretViews;

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
        this.secretViews = null;
    }

    /**
     * Plain Secret-detailed Site view.
     * @param id
     * @param name
     * @param secretViews
     */
    public SiteView(final String id,final String name,
            final Map<String, SecretView> secretViews) {
        this.id = id;
        this.name = name;
        this.configured = secretViews.values().stream().anyMatch(secretView -> null != secretView.getSecret());
        this.secretViews = secretViews;
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
    public Map<String, SecretView> getSecretViews() {
        return secretViews;
    }
}
