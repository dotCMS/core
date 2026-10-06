package com.dotcms.auth.dotAuth.rest;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Body of {@code PUT /v1/dotauth/sites/{hostId}}. Values mirror the secret
 * keys for the chosen {@code protocol}. A hidden-secret value of {@code "****"}
 * is treated by the resource as "preserve the stored value".
 *
 * <p>If {@code protocol} is absent from the JSON body the form falls back to
 * {@link DotAuthProtocol#OAUTH}, preserving the phase-2 wire contract.
 */
public class DotAuthConfigForm extends Validated {

    private final DotAuthProtocol protocol;

    @NotNull
    private final Map<String, Object> values;

    @JsonCreator
    public DotAuthConfigForm(@JsonProperty("protocol") final DotAuthProtocol protocol,
                             @JsonProperty("values")   final Map<String, Object> values) {
        this.protocol = protocol == null ? DotAuthProtocol.OAUTH : protocol;
        this.values = values;
    }

    public DotAuthProtocol getProtocol() {
        return protocol;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
