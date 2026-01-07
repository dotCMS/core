package com.dotcms.rest.api.v1.serviceauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Status information for service authentication.
 *
 * @author dotCMS
 */
@Schema(description = "Status of service-to-service authentication")
public class ServiceAuthStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Whether service authentication is enabled", example = "true")
    private final boolean enabled;

    @Schema(description = "Whether the service is ready to issue/validate tokens", example = "true")
    private final boolean ready;

    @Schema(description = "Status message", example = "Service authentication is enabled and ready")
    private final String message;

    public ServiceAuthStatus(final boolean enabled, final boolean ready, final String message) {
        this.enabled = enabled;
        this.ready = ready;
        this.message = message;
    }

    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("ready")
    public boolean isReady() {
        return ready;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }
}
