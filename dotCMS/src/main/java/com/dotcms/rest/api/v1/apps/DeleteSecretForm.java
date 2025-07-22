package com.dotcms.rest.api.v1.apps;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Form that allows deleting a set of secrets/params
 */
public class DeleteSecretForm extends Validated {

    @NotNull
    private final String key;

    @NotNull
    private final String siteId;

    @NotNull
    private final Set<String> params;

    @JsonCreator
    public DeleteSecretForm(@JsonProperty("key") final String key, @JsonProperty("siteId") final String siteId, @JsonProperty("params") final Set<String> params) {
        super();
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
     * Param names passed to delete
     * @return
     */
    public Set<String> getParams() {
        return params;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }
}
