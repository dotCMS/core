package com.dotcms.auth.providers.jwt.services.serviceauth.example;

import com.dotcms.auth.providers.jwt.services.serviceauth.ServiceAuthClient;
import com.dotcms.auth.providers.jwt.services.serviceauth.ServiceCredential;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Example: Using ServiceAuthClient to call an accessibility checker service.
 *
 * This demonstrates how dotCMS would integrate with an external service like wa11y
 * for checking page accessibility. The pattern applies to any microservice integration.
 *
 * <h2>Setup Required:</h2>
 * <ol>
 *   <li>Set {@code SERVICE_AUTH_ENABLED=true} in configuration</li>
 *   <li>Ensure the external service can validate JWTs with the same signing key</li>
 *   <li>Configure network access between dotCMS pods and the service</li>
 * </ol>
 *
 * <h2>Kubernetes Deployment:</h2>
 * <pre>
 * # Both services share the same signing key via K8s secret
 * apiVersion: v1
 * kind: Secret
 * metadata:
 *   name: service-auth-key
 * stringData:
 *   signing-key: "your-256-bit-secret-key-here"
 * ---
 * # Reference in both deployments:
 * env:
 *   - name: JSON_WEB_TOKEN_SECRET
 *     valueFrom:
 *       secretKeyRef:
 *         name: service-auth-key
 *         key: signing-key
 * </pre>
 *
 * @author dotCMS
 */
public class AccessibilityCheckerExample {

    // Define the service once, reuse everywhere
    private static final ServiceCredential WA11Y_SERVICE = ServiceCredential.builder()
            .serviceId("wa11y-checker")
            .displayName("Accessibility Checker")
            .baseUrl("https://wa11y.internal.dotcms.cloud")
            .tokenExpirationSeconds(300)  // 5 minute tokens
            .build();

    /**
     * Check a page for accessibility issues.
     *
     * @param pageUrl The URL of the page to check
     * @return Accessibility check results
     */
    public AccessibilityResult checkPage(final String pageUrl)
            throws DotDataException, DotSecurityException {

        final ServiceAuthClient client = new ServiceAuthClient(WA11Y_SERVICE);

        // The JWT is automatically generated and injected into the request
        return client.post(
                "/api/v1/check",
                Map.of(
                        "url", pageUrl,
                        "standard", "WCAG2AA",
                        "timeout", 30000
                ),
                AccessibilityResult.class
        );
    }

    /**
     * Check multiple pages in batch.
     */
    public BatchResult checkPages(final List<String> pageUrls)
            throws DotDataException, DotSecurityException {

        final ServiceAuthClient client = new ServiceAuthClient(WA11Y_SERVICE);

        return client.post(
                "/api/v1/batch-check",
                Map.of("urls", pageUrls, "standard", "WCAG2AA"),
                BatchResult.class
        );
    }

    /**
     * Alternative: Register services at startup for cleaner code.
     */
    public static void registerServices() {
        // Register services once at startup (e.g., in a StartupListener)
        ServiceAuthClient.registerService(WA11Y_SERVICE);

        ServiceAuthClient.registerService(ServiceCredential.builder()
                .serviceId("analytics-service")
                .displayName("Analytics Service")
                .baseUrl("https://analytics.internal.dotcms.cloud")
                .build());

        ServiceAuthClient.registerService(ServiceCredential.builder()
                .serviceId("ai-service")
                .displayName("AI Content Service")
                .baseUrl("https://ai.internal.dotcms.cloud")
                .tokenExpirationSeconds(60)  // Shorter tokens for sensitive service
                .build());

        Logger.info(AccessibilityCheckerExample.class, "Service credentials registered");
    }

    /**
     * Use registered services by ID.
     */
    public void useRegisteredService() throws DotDataException, DotSecurityException {
        // Look up service by ID instead of passing credential around
        final ServiceAuthClient client = ServiceAuthClient.forService("wa11y-checker");

        final AccessibilityResult result = client.post(
                "/api/v1/check",
                Map.of("url", "https://example.com"),
                AccessibilityResult.class
        );

        Logger.info(this, "Accessibility check found " + result.getIssueCount() + " issues");
    }

    // --- Response DTOs ---

    /**
     * Example response from accessibility checker.
     */
    public static class AccessibilityResult implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("url")
        private String url;

        @JsonProperty("issues")
        private List<AccessibilityIssue> issues;

        @JsonProperty("score")
        private int score;

        @JsonProperty("standard")
        private String standard;

        public String getUrl() { return url; }
        public List<AccessibilityIssue> getIssues() { return issues; }
        public int getScore() { return score; }
        public String getStandard() { return standard; }

        public int getIssueCount() {
            return issues != null ? issues.size() : 0;
        }
    }

    /**
     * Individual accessibility issue.
     */
    public static class AccessibilityIssue implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("code")
        private String code;

        @JsonProperty("type")
        private String type;  // error, warning, notice

        @JsonProperty("message")
        private String message;

        @JsonProperty("selector")
        private String selector;

        @JsonProperty("context")
        private String context;

        public String getCode() { return code; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getSelector() { return selector; }
        public String getContext() { return context; }
    }

    /**
     * Batch check result.
     */
    public static class BatchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("results")
        private List<AccessibilityResult> results;

        @JsonProperty("totalIssues")
        private int totalIssues;

        public List<AccessibilityResult> getResults() { return results; }
        public int getTotalIssues() { return totalIssues; }
    }
}
