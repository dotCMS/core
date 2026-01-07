/**
 * Service-to-Service Authentication Package
 *
 * <p>This package provides JWT-based authentication for communication between dotCMS
 * and external microservices. It enables secure, stateless authentication without
 * requiring a central identity provider.</p>
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌──────────────┐                      ┌──────────────────┐
 * │   dotCMS     │  JWT (Bearer token)  │  Microservice    │
 * │   Instance   │ ──────────────────▶  │  (wa11y, etc)    │
 * └──────────────┘                      └──────────────────┘
 *        │                                      │
 *        │ Signs with shared key                │ Validates with same key
 *        ▼                                      ▼
 * ┌──────────────────────────────────────────────────────────┐
 * │              Shared Signing Key (K8s Secret)             │
 * └──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Quick Start</h2>
 * <pre>
 * // 1. Define a service credential
 * ServiceCredential wa11y = ServiceCredential.builder()
 *     .serviceId("wa11y-checker")
 *     .baseUrl("https://wa11y.internal.example.com")
 *     .tokenExpirationSeconds(300)
 *     .build();
 *
 * // 2. Create a client
 * ServiceAuthClient client = new ServiceAuthClient(wa11y);
 *
 * // 3. Make authenticated requests
 * AccessibilityResult result = client.post(
 *     "/api/check",
 *     Map.of("url", "https://example.com/page"),
 *     AccessibilityResult.class
 * );
 * </pre>
 *
 * <h2>Configuration</h2>
 * Add these to dotmarketing-config.properties or environment variables:
 * <pre>
 * # Enable service authentication (required)
 * SERVICE_AUTH_ENABLED=true
 *
 * # JWT settings
 * SERVICE_AUTH_JWT_EXPIRATION_SECONDS=300
 * SERVICE_AUTH_ISSUER=my-dotcms-cluster
 *
 * # HTTP client settings
 * SERVICE_AUTH_HTTP_TIMEOUT_MS=30000
 * SERVICE_AUTH_RETRY_ATTEMPTS=3
 *
 * # Optional: Custom signing key factory
 * SERVICE_AUTH_SIGNING_KEY_FACTORY=com.dotcms.auth.providers.jwt.factories.impl.SecretKeySpecFactoryImpl
 * </pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.dotcms.auth.providers.jwt.services.serviceauth.ServiceCredential} -
 *       Immutable bean representing a service identity</li>
 *   <li>{@link com.dotcms.auth.providers.jwt.services.serviceauth.ServiceJwtService} -
 *       Core service for JWT generation and validation</li>
 *   <li>{@link com.dotcms.auth.providers.jwt.services.serviceauth.ServiceAuthClient} -
 *       High-level HTTP client with automatic JWT injection</li>
 *   <li>{@link com.dotcms.auth.providers.jwt.services.serviceauth.ServiceTokenClaims} -
 *       Validated claims from incoming JWTs</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>JWTs are short-lived (default 5 minutes) to limit exposure</li>
 *   <li>Uses the same signing key infrastructure as user JWTs</li>
 *   <li>Supports audience validation to prevent token misuse</li>
 *   <li>All requests use HTTPS (enforced by circuit breaker)</li>
 *   <li>No credentials stored in code - use K8s secrets or config</li>
 * </ul>
 *
 * <h2>Receiving Service Implementation</h2>
 * <p>For services that receive authenticated calls from dotCMS, validate tokens like this:</p>
 * <pre>
 * // In your microservice (pseudo-code for any language)
 * String token = request.getHeader("Authorization").replace("Bearer ", "");
 *
 * // Option A: Validate locally if you have the shared signing key
 * Claims claims = Jwts.parser()
 *     .setSigningKey(sharedKey)
 *     .parseClaimsJws(token)
 *     .getBody();
 *
 * // Option B: Call back to dotCMS validation endpoint (more secure for external services)
 * POST /api/v1/service-auth/validate
 * { "token": "...", "expectedAudience": "wa11y-checker" }
 * </pre>
 *
 * @author dotCMS
 * @since 24.x
 */
package com.dotcms.auth.providers.jwt.services.serviceauth;
