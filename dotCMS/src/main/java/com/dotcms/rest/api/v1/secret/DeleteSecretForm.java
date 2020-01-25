package com.dotcms.rest.api.v1.secret;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import java.util.Set;

/**
 * Form that allows deleting a set of secrets/params
 */
public class DeleteSecretForm extends Validated {

    @NotNull
    private String key;

    @NotNull
    private String siteId;

    @NotNull
    private Set<String> params;

    /**
     * This should contain the unique identifier that differentiates the service
     * @return
     */
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * This should contain the site-id the config belongs to
     * @return
     */
    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    /**
     * Param names passed to delete
     * @return
     */
    public Set<String> getParams() {
        return params;
    }

    public void setParams(Set<String> params) {
        this.params = params;
    }
}
