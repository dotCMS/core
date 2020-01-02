package com.dotcms.rest.api.v1.secret;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.security.secret.Param;
import java.util.Map;

public class SecretForm extends Validated {

    @NotNull
    private String serviceKey;

    @NotNull
    private String hostId;

    @NotNull
    private Map<String, Param> params;

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public Map<String, Param> getParams() {
        return params;
    }

    public void setParams(Map<String, Param> params) {
        this.params = params;
    }

}
