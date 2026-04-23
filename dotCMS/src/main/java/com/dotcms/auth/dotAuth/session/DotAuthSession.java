package com.dotcms.auth.dotAuth.session;

import java.io.Serializable;

/**
 * In-memory session record for the dotAuth OAuth-exchange flow. Stored in
 * {@link DotAuthSessionCache} and keyed by an opaque, high-entropy session-ref
 * that the SPA returns on subsequent calls as a {@code Authorization: Bearer}
 * credential.
 *
 * <p>Intentionally ephemeral: this is the headless analogue of a servlet
 * HTTP session for the OIDC exchange endpoint. Loss of the underlying cache
 * entry (node restart, eviction, cluster flush, explicit logout) invalidates
 * the session and forces the SPA to re-authenticate with the IdP — the same
 * re-auth contract the SAML browser flow has today.
 *
 * <p>No fields are persisted to the database. This object is Serializable so
 * that Hazelcast-backed cache providers can replicate it across nodes in a
 * cluster; nothing more.
 */
public final class DotAuthSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final long   createdAt;
    private final long   expiresAt;

    public DotAuthSession(final String userId,
                          final long createdAt,
                          final long expiresAt) {
        this.userId    = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getUserId()    { return userId; }
    public long   getCreatedAt() { return createdAt; }
    public long   getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
