package com.dotcms.auth.providers.jwt.services.serviceauth;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Represents the validated claims from a service-to-service JWT.
 * Returned by {@link ServiceJwtService#validateToken(String, String)}.
 *
 * @author dotCMS
 */
@Value.Immutable
@JsonSerialize(as = ImmutableServiceTokenClaims.class)
@JsonDeserialize(as = ImmutableServiceTokenClaims.class)
public abstract class ServiceTokenClaims {

    /**
     * Unique token identifier (jti claim)
     */
    @Nullable
    public abstract String tokenId();

    /**
     * The service ID that this token was issued for (sid claim)
     */
    @Nullable
    public abstract String serviceId();

    /**
     * The issuer of the token (iss claim)
     */
    @Nullable
    public abstract String issuer();

    /**
     * The intended audience/target service (aud claim)
     */
    @Nullable
    public abstract String audience();

    /**
     * The source cluster that issued this token (src claim)
     */
    @Nullable
    public abstract String sourceCluster();

    /**
     * When the token was issued (iat claim)
     */
    @Nullable
    public abstract Date issuedAt();

    /**
     * When the token expires (exp claim)
     */
    @Nullable
    public abstract Date expiresAt();

    /**
     * Check if this token is expired.
     */
    public boolean isExpired() {
        return expiresAt() != null && expiresAt().before(new Date());
    }

    public static ImmutableServiceTokenClaims.Builder builder() {
        return ImmutableServiceTokenClaims.builder();
    }
}
