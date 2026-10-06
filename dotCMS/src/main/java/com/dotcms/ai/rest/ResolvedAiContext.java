package com.dotcms.ai.rest;

import com.dotcms.ai.app.AppConfig;
import com.dotmarketing.beans.Host;
import com.liferay.portal.model.User;

/**
 * The caller, the site their request resolved to, and that site's dotAI configuration — resolved
 * together, in one step.
 *
 * <p>The three are returned as a unit on purpose. Obtaining them separately is what lets a caller
 * authorize against one site and then fetch configuration for another, which is not a theoretical
 * risk: two shipped dotAI endpoints have already drifted to opposite model-passthrough policies
 * by each making the decision for themselves. A type that can only be produced by the shared
 * resolver removes the opportunity.</p>
 *
 * @param user   the authenticated caller
 * @param host   the site the request resolved to, after standard dotCMS host resolution
 * @param config that site's dotAI configuration, including any system-level inheritance
 */
public record ResolvedAiContext(User user, Host host, AppConfig config) {

    public ResolvedAiContext {
        if (user == null) {
            throw new IllegalArgumentException("ResolvedAiContext user is required");
        }
        if (host == null) {
            throw new IllegalArgumentException("ResolvedAiContext host is required");
        }
        if (config == null) {
            throw new IllegalArgumentException("ResolvedAiContext config is required");
        }
    }

    /**
     * The identity to report as having served — and paid for — the request.
     *
     * @return the resolved site's identifier
     */
    public String servingSiteId() {
        return host.getIdentifier();
    }

    /**
     * @return whether the resolved site, or the system level it inherits from, has any dotAI
     *         configuration at all
     */
    public boolean isConfigured() {
        return config != null && config.isEnabled();
    }
}
