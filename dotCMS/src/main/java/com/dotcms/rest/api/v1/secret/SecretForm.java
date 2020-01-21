package com.dotcms.rest.api.v1.secret;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.security.secret.Param;
import java.util.Map;

public class SecretForm extends Validated {

    @NotNull
    private String serviceKey;

    @NotNull
    private String siteId;

    @NotNull
    private Map<String, Param> params;

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(final String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(final String siteId) {
        this.siteId = siteId;
    }

    public Map<String, Param> getParams() {
        return params;
    }

    public void setParams(final Map<String, Param> params) {
        this.params = params;
    }

}
