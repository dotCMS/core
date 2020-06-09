package com.dotcms.rest.api.v1.apps.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

/**
 * Represents the site and the secrets associated to it
 * Optionally The secrets can be null. In such case the view will only represent plain site info.
 */
public class SiteView {

    private final String id;
    private final String name;
    private final boolean configured;

    @JsonInclude(Include.NON_NULL)
    private final Integer secretsWithWarnings;

    @JsonInclude(Include.NON_NULL)
    private final List<SecretView> secrets;

    /**
     * If we want to build a secret-less view but showing that the site has integrations.
     * @param id
     * @param name
     * @param configured
     */
    public SiteView(final String id, final String name, final boolean configured, final int secretsWithWarning) {
        this.id = id;
        this.name = name;
        this.configured = configured;
        this.secrets = null;
        this.secretsWithWarnings = (secretsWithWarning == 0 ? null : secretsWithWarning);
    }

    /**
     * Plain Secret-detailed Site view.
     * @param id
     * @param name
     * @param secrets
     */
    public SiteView(final String id,final String name,
            final List<SecretView> secrets) {
        this.id = id;
        this.name = name;
        this.configured = secrets.stream().anyMatch(secretView -> null != secretView.getSecret());
        this.secrets = secrets;
        this.secretsWithWarnings = null;
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
     * Number of Secrets in conflict that have generated Warnings
     * @return
     */
    public Integer getSecretsWithWarnings() {
        return secretsWithWarnings;
    }

    /**
     * Secrets per site
     * @return
     */
    public List<SecretView> getSecrets() {
        return secrets;
    }
}
