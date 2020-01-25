package com.dotcms.rest.api.v1.secret;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.security.secret.Param;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Form used to feed-in secrets
 */
public class SecretForm extends Validated {

    @NotNull
    private final String key;

    @NotNull
    private final String siteId;

    @NotNull
    private final Map<String, Param> params;

    @JsonCreator
    public SecretForm(@JsonProperty("key") final String key,
            @JsonProperty("siteId") final String siteId,
            @JsonProperty("params") final Map<String, Param> params) {
        this.key = key;
        this.siteId = siteId;
        this.params = params;
    }

    /**
     * This should contain the unique identifier that differentiates the service
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * This should contain the site-id the config belongs to
     * @return
     */
    public String getSiteId() {
        return siteId;
    }

    /**
     * Param Name and Value Map
     * @return
     */
    public Map<String, Param> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }
}
