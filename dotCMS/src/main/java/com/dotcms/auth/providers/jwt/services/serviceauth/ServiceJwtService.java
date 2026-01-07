package com.dotcms.auth.providers.jwt.services.serviceauth;

import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.jsonwebtoken.*;

import java.io.Serializable;
import java.security.Key;
import java.util.*;

/**
 * Service for generating and validating JWTs for service-to-service authentication.
 *
 * This provides a simple, stateless authentication mechanism between dotCMS and
 * external microservices. JWTs are short-lived (default 5 minutes) and contain
 * service-specific claims.
 *
 * <h2>Configuration Properties:</h2>
 * <ul>
 *   <li>{@code SERVICE_AUTH_ENABLED} - Enable/disable service auth (default: false)</li>
 *   <li>{@code SERVICE_AUTH_JWT_EXPIRATION_SECONDS} - Default token TTL (default: 300)</li>
 *   <li>{@code SERVICE_AUTH_ISSUER} - JWT issuer claim (default: cluster ID)</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * ServiceJwtService jwtService = ServiceJwtService.getInstance();
 *
 * // Generate a token for calling wa11y service
 * String token = jwtService.generateToken(
 *     ServiceCredential.builder()
 *         .serviceId("wa11y-checker")
 *         .baseUrl("https://wa11y.example.com")
 *         .build()
 * );
 *
 * // Use the token in HTTP calls
 * CircuitBreakerUrl.builder()
 *     .setUrl("https://wa11y.example.com/check")
 *     .setHeaders(ServiceJwtService.authHeaders(token))
 *     .build()
 *     .doString();
 * </pre>
 *
 * @author dotCMS
 */
public class ServiceJwtService implements Serializable {

    private static final long serialVersionUID = 1L;

    // Configuration keys
    public static final String SERVICE_AUTH_ENABLED = "SERVICE_AUTH_ENABLED";
    public static final String SERVICE_AUTH_JWT_EXPIRATION_SECONDS = "SERVICE_AUTH_JWT_EXPIRATION_SECONDS";
    public static final String SERVICE_AUTH_ISSUER = "SERVICE_AUTH_ISSUER";
    public static final String SERVICE_AUTH_SIGNING_KEY_FACTORY = "SERVICE_AUTH_SIGNING_KEY_FACTORY";

    // Default values
    private static final int DEFAULT_EXPIRATION_SECONDS = 300; // 5 minutes
    private static final String DEFAULT_SIGNING_KEY_FACTORY =
            "com.dotcms.auth.providers.jwt.factories.impl.SecretKeySpecFactoryImpl";

    // JWT claim names
    public static final String CLAIM_SERVICE_ID = "sid";
    public static final String CLAIM_TARGET_SERVICE = "aud";
    public static final String CLAIM_SOURCE_CLUSTER = "src";

    private volatile Key signingKey;
    private volatile String issuerId;

    private ServiceJwtService() {
        // singleton
    }

    private static class SingletonHolder {
        private static final ServiceJwtService INSTANCE = new ServiceJwtService();
    }

    /**
     * Get the singleton instance.
     */
    public static ServiceJwtService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Check if service-to-service authentication is enabled.
     */
    public boolean isEnabled() {
        return Config.getBooleanProperty(SERVICE_AUTH_ENABLED, false);
    }

    /**
     * Generate a JWT for authenticating with a target service.
     *
     * @param targetService The service credential for the target service
     * @return A signed JWT string
     * @throws DotSecurityException if service auth is disabled or key unavailable
     */
    public String generateToken(final ServiceCredential targetService) throws DotSecurityException {
        if (!isEnabled()) {
            throw new DotSecurityException("Service-to-service authentication is not enabled. " +
                    "Set SERVICE_AUTH_ENABLED=true in configuration.");
        }

        if (targetService == null || !UtilMethods.isSet(targetService.serviceId())) {
            throw new DotSecurityException("Target service credential is required");
        }

        if (!targetService.enabled()) {
            throw new DotSecurityException("Service credential for '" +
                    targetService.serviceId() + "' is disabled");
        }

        final Date now = new Date();
        final int expirationSeconds = targetService.tokenExpirationSeconds() > 0
                ? targetService.tokenExpirationSeconds()
                : Config.getIntProperty(SERVICE_AUTH_JWT_EXPIRATION_SECONDS, DEFAULT_EXPIRATION_SECONDS);
        final Date expiration = new Date(now.getTime() + (expirationSeconds * 1000L));

        try {
            final JwtBuilder builder = Jwts.builder()
                    .setId(UUID.randomUUID().toString())
                    .setSubject("dotcms-service-auth")
                    .setIssuer(getIssuer())
                    .setAudience(targetService.audience())
                    .setIssuedAt(now)
                    .setNotBefore(now)
                    .setExpiration(expiration)
                    .claim(CLAIM_SERVICE_ID, targetService.serviceId())
                    .claim(CLAIM_SOURCE_CLUSTER, ClusterFactory.getClusterId())
                    .setHeaderParam("typ", "JWT");

            builder.signWith(getSignatureAlgorithm(), getSigningKey());

            final String token = builder.compact();

            Logger.debug(this, () -> String.format(
                    "Generated service JWT for target=%s, expires=%s",
                    targetService.serviceId(), expiration));

            return token;

        } catch (Exception e) {
            Logger.error(this, "Failed to generate service JWT: " + e.getMessage(), e);
            throw new DotSecurityException("Failed to generate service token", e);
        }
    }

    /**
     * Validate an incoming service JWT.
     * Use this to validate tokens from other dotCMS instances or authorized services.
     *
     * @param token The JWT string to validate
     * @param expectedAudience The expected audience (your service ID)
     * @return The validated claims
     * @throws DotSecurityException if validation fails
     */
    public ServiceTokenClaims validateToken(final String token, final String expectedAudience)
            throws DotSecurityException {

        if (!UtilMethods.isSet(token)) {
            throw new DotSecurityException("Token is required");
        }

        try {
            final Jws<Claims> jws = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token);

            final Claims body = jws.getBody();

            // Validate algorithm - reject none/null
            final String algo = jws.getHeader().getAlgorithm();
            if (!UtilMethods.isSet(algo) || "none".equalsIgnoreCase(algo)) {
                throw new DotSecurityException("Invalid JWT algorithm");
            }

            // Validate audience if specified
            if (UtilMethods.isSet(expectedAudience) &&
                    !expectedAudience.equals(body.getAudience())) {
                throw new DotSecurityException("JWT audience mismatch. Expected: " +
                        expectedAudience + ", got: " + body.getAudience());
            }

            // Check expiration
            if (body.getExpiration() != null && body.getExpiration().before(new Date())) {
                throw new DotSecurityException("JWT has expired");
            }

            return ImmutableServiceTokenClaims.builder()
                    .tokenId(body.getId())
                    .serviceId(body.get(CLAIM_SERVICE_ID, String.class))
                    .issuer(body.getIssuer())
                    .audience(body.getAudience())
                    .sourceCluster(body.get(CLAIM_SOURCE_CLUSTER, String.class))
                    .issuedAt(body.getIssuedAt())
                    .expiresAt(body.getExpiration())
                    .build();

        } catch (ExpiredJwtException e) {
            throw new DotSecurityException("JWT has expired", e);
        } catch (SignatureException e) {
            throw new DotSecurityException("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            throw new DotSecurityException("Malformed JWT", e);
        } catch (DotSecurityException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "JWT validation failed: " + e.getMessage(), e);
            throw new DotSecurityException("JWT validation failed", e);
        }
    }

    /**
     * Create authorization headers map for HTTP requests.
     *
     * @param token The JWT token
     * @return Map of headers including Authorization Bearer token
     */
    public static Map<String, String> authHeaders(final String token) {
        return Map.of(
                "Authorization", "Bearer " + token,
                "Accept", "application/json",
                "Content-Type", "application/json",
                "X-Service-Auth", "dotcms"
        );
    }

    /**
     * Extract Bearer token from Authorization header value.
     *
     * @param authHeader The Authorization header value
     * @return The token without "Bearer " prefix, or null if invalid
     */
    public static String extractBearerToken(final String authHeader) {
        if (UtilMethods.isSet(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Key getSigningKey() throws DotSecurityException {
        if (signingKey == null) {
            synchronized (this) {
                if (signingKey == null) {
                    final String factoryClass = Config.getStringProperty(
                            SERVICE_AUTH_SIGNING_KEY_FACTORY, DEFAULT_SIGNING_KEY_FACTORY);

                    final SigningKeyFactory factory = (SigningKeyFactory)
                            ReflectionUtils.newInstance(factoryClass);

                    if (factory == null) {
                        throw new DotSecurityException(
                                "Could not instantiate signing key factory: " + factoryClass);
                    }

                    signingKey = factory.getKey();

                    if (signingKey == null) {
                        throw new DotSecurityException("Signing key is not configured");
                    }
                }
            }
        }
        return signingKey;
    }

    private SignatureAlgorithm getSignatureAlgorithm() throws DotSecurityException {
        final String algoFromKey = getSigningKey().getAlgorithm();
        return Arrays.stream(SignatureAlgorithm.values())
                .filter(algo -> algoFromKey.equals(algo.getJcaName()))
                .findFirst()
                .orElse(SignatureAlgorithm.HS256);
    }

    private String getIssuer() {
        if (issuerId == null) {
            issuerId = Config.getStringProperty(SERVICE_AUTH_ISSUER, ClusterFactory.getClusterId());
        }
        return issuerId;
    }
}
