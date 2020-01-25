package com.dotcms.rest.api.v1.secret;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.security.secret.Param;
import java.util.Map;

/**
 * Form used to feed-in secrets
 */
public class SecretForm extends Validated {


    @NotNull
    private String key;

    @NotNull
    private String siteId;

    @NotNull
    private Map<String, Param> params;

    /**
     * This should contain the unique identifier that differentiates the service
     * @return
     */
    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * This should contain the site-id the config belongs to
     * @return
     */
    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(final String siteId) {
        this.siteId = siteId;
    }

    /**
     * Param Name and Value Map
     * @return
     */
    public Map<String, Param> getParams() {
        return params;
    }

    public void setParams(final Map<String, Param> params) {
        this.params = params;
    }

}
