package com.dotcms.auth.dotAuth.session;

import java.util.Optional;

/**
 * Minimal store API for dotAuth OAuth-exchange sessions. The wire credential
 * (returned to the SPA) is an opaque high-entropy string; the store maps it to
 * a {@link DotAuthSession} carrying the resolved dotCMS user id plus lifetime
 * metadata.
 */
public interface DotAuthSessionCache {

    /** Result of a rotate-on-use lookup: the new ref + the session data. */
    final class RotatedSession {
        private final String newRef;
        private final DotAuthSession session;

        public RotatedSession(final String newRef, final DotAuthSession session) {
            this.newRef = newRef;
            this.session = session;
        }

        public String getNewRef() { return newRef; }
        public DotAuthSession getSession() { return session; }
    }

    /**
     * Prefix that identifies a dotAuth session-ref on the wire. Distinguishes
     * session-refs from dotCMS JWTs so the auth chain can short-circuit to the
     * session lookup without attempting a JWT parse first.
     */
    String SESSION_REF_PREFIX = "dsr_";

    /**
     * Mint and store a new session-ref for the given user, valid for
     * {@code lifetimeMillis} from now. Returns the session-ref string the
     * caller should hand back to the SPA.
     */
    String create(String userId, long lifetimeMillis);

    /**
     * Look up the session referred to by {@code sessionRef}. Returns empty
     * when the ref is unknown, expired, or does not carry the expected prefix.
     * Expired entries are evicted as a side effect of this lookup so the cache
     * self-heals under load rather than leaning entirely on provider-side TTL.
     */
    Optional<DotAuthSession> get(String sessionRef);

    /**
     * Atomically look up, invalidate, and re-mint the session under a new ref.
     * Returns the new session-ref + session, or empty if the original ref was
     * invalid/expired. Used when rotate-on-use is enabled — each API call
     * consumes the ref and gets a fresh one back via response header.
     */
    Optional<RotatedSession> getAndRotate(String sessionRef);

    /** Remove a session-ref (logout). No-op when the ref is unknown. */
    void invalidate(String sessionRef);

    /** Remove every active dotAuth session-ref. Used by dotAuth emergency controls. */
    void invalidateAll();
}
