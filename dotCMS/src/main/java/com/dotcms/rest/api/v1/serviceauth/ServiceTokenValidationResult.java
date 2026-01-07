package com.dotcms.rest.api.v1.serviceauth;

import com.dotcms.auth.providers.jwt.services.serviceauth.ServiceTokenClaims;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Result of service token validation.
 *
 * @author dotCMS
 */
@Schema(description = "Result of service token validation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceTokenValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Whether the token is valid", example = "true")
    private final boolean valid;

    @Schema(description = "The validated token claims (only present if valid)")
    private final ServiceTokenClaimsView claims;

    @Schema(description = "Error message if validation failed")
    private final String error;

    public ServiceTokenValidationResult(
            final boolean valid,
            final ServiceTokenClaims claims,
            final String error) {
        this.valid = valid;
        this.claims = claims != null ? new ServiceTokenClaimsView(claims) : null;
        this.error = error;
    }

    @JsonProperty("valid")
    public boolean isValid() {
        return valid;
    }

    @JsonProperty("claims")
    public ServiceTokenClaimsView getClaims() {
        return claims;
    }

    @JsonProperty("error")
    public String getError() {
        return error;
    }

    /**
     * View class for ServiceTokenClaims to control JSON serialization.
     */
    @Schema(description = "Validated claims from the service JWT")
    public static class ServiceTokenClaimsView implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "Unique token identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        private final String tokenId;

        @Schema(description = "The service ID this token was issued for", example = "wa11y-checker")
        private final String serviceId;

        @Schema(description = "The issuer (dotCMS cluster ID)", example = "dotcms-cluster-1")
        private final String issuer;

        @Schema(description = "The intended audience/target service", example = "wa11y-checker")
        private final String audience;

        @Schema(description = "The source cluster that issued this token", example = "dotcms-cluster-1")
        private final String sourceCluster;

        @Schema(description = "When the token was issued (epoch ms)", example = "1705315800000")
        private final Long issuedAt;

        @Schema(description = "When the token expires (epoch ms)", example = "1705316100000")
        private final Long expiresAt;

        public ServiceTokenClaimsView(final ServiceTokenClaims claims) {
            this.tokenId = claims.tokenId();
            this.serviceId = claims.serviceId();
            this.issuer = claims.issuer();
            this.audience = claims.audience();
            this.sourceCluster = claims.sourceCluster();
            this.issuedAt = claims.issuedAt() != null ? claims.issuedAt().getTime() : null;
            this.expiresAt = claims.expiresAt() != null ? claims.expiresAt().getTime() : null;
        }

        public String getTokenId() { return tokenId; }
        public String getServiceId() { return serviceId; }
        public String getIssuer() { return issuer; }
        public String getAudience() { return audience; }
        public String getSourceCluster() { return sourceCluster; }
        public Long getIssuedAt() { return issuedAt; }
        public Long getExpiresAt() { return expiresAt; }
    }
}
