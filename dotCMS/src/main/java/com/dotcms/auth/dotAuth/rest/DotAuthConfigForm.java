package com.dotcms.auth.dotAuth.rest;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Body of {@code PUT /v1/dotauth/sites/{hostId}}. Values mirror the
 * {@code OAuthAppConfig} secret keys. A {@code clientSecret} value of
 * {@code "****"} is treated by the resource as "preserve the stored value".
 */
public class DotAuthConfigForm extends Validated {

    @NotNull
    private final Map<String, Object> values;

    @JsonCreator
    public DotAuthConfigForm(@JsonProperty("values") final Map<String, Object> values) {
        this.values = values;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
