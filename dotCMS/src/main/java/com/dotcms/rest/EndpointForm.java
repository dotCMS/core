package com.dotcms.rest;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates the data of an endpoint on an environment
 * @author jsanca
 */
public class EndpointForm  extends Validated implements java.io.Serializable {

    @NotNull
    @JsonProperty("name")
    private final String name;

    @NotNull
    @JsonProperty("protocol")
    private final String protocol;

    @NotNull
    @JsonProperty("address")
    private final String address;

    @NotNull
    @JsonProperty("port")
    private final String port;

    @JsonProperty("authorizationToken")
    private final String authorizationToken;

    @JsonProperty("enabled")
    private final boolean enabled;

    @JsonProperty("sending")
    private final boolean sending;

    @NotNull
    @JsonProperty("environmentId")
    private final String environmentId;

    @JsonCreator
    public EndpointForm(@JsonProperty("name") final String name,
            @JsonProperty("protocol") final String protocol,
            @JsonProperty("address") final String address,
            @JsonProperty("port") final String port,
            @JsonProperty("authorizationToken") final String authorizationToken,
            @JsonProperty("enabled") final boolean enabled,
            @JsonProperty("sending") final boolean sending,
            @JsonProperty("environmentId") final String environmentId
    ) {
        this.name = name;
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.authorizationToken = authorizationToken;
        this.enabled = enabled;
        this.sending = sending;
        this.environmentId = environmentId;
        checkValid();
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAddress() {
        return address;
    }

    public String getPort() {
        return port;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public boolean isSending() {
        return sending;
    }
}
