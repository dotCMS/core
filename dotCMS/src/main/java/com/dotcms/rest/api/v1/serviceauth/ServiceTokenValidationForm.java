package com.dotcms.rest.api.v1.serviceauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Form for validating a service-to-service JWT token.
 *
 * @author dotCMS
 */
@Schema(description = "Request to validate a service-to-service JWT token")
public class ServiceTokenValidationForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(
            description = "The JWT token to validate",
            required = true,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private final String token;

    @Schema(
            description = "Expected audience claim. If provided, validation will fail " +
                    "if the token's audience doesn't match. This ensures tokens " +
                    "intended for one service can't be used with another.",
            example = "wa11y-checker"
    )
    private final String expectedAudience;

    @JsonCreator
    public ServiceTokenValidationForm(
            @JsonProperty("token") final String token,
            @JsonProperty("expectedAudience") final String expectedAudience) {
        this.token = token;
        this.expectedAudience = expectedAudience;
    }

    public String getToken() {
        return token;
    }

    public String getExpectedAudience() {
        return expectedAudience;
    }
}
