package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.Secret;
import com.dotcms.security.secret.ServiceSecrets;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

public class ServiceIntegrationDetailedView {

    private final ServiceIntegrationView service;

    private final HostView host;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, Secret> secrets;

    public ServiceIntegrationDetailedView(
            final ServiceIntegrationView service,
            final HostView host,
            final ServiceSecrets serviceSecrets) {
        this.service = service;
        this.host = host;
        this.secrets = serviceSecrets.getSecrets();
    }

    public ServiceIntegrationView getService() {
        return service;
    }

    public HostView getHost() {
        return host;
    }

    public Map<String, Secret> getSecrets() {
        return secrets;
    }
}
