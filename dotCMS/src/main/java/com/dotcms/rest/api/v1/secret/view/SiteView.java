package com.dotcms.rest.api.v1.secret.view;

import com.dotcms.security.secret.Secret;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

public class SiteView {

    private final String id;
    private final String name;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, Secret> secrets;

    public SiteView(final String id, final String name) {
        this.id = id;
        this.name = name;
        this.secrets = null;
    }

    public SiteView(final String id,final String name,
            final Map<String, Secret> secrets) {
        this.id = id;
        this.name = name;
        this.secrets = secrets;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Secret> getSecrets() {
        return secrets;
    }
}
