package com.dotcms.auth.dotAuth.session;

import java.util.Optional;

/**
 * Minimal store API for dotAuth OAuth-exchange sessions. The wire credential
 * (returned to the SPA) is an opaque high-entropy string; the store maps it to
 * a {@link DotAuthSession} carrying the resolved dotCMS user id plus lifetime
 * metadata.
 */
public interface DotAuthSessionCache {

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

    /** Remove a session-ref (logout). No-op when the ref is unknown. */
    void invalidate(String sessionRef);

    /** Remove every active dotAuth session-ref. Used by dotAuth emergency controls. */
    void invalidateAll();

    /**
     * One-time-use guard for exchanged {@code id_token}s. Records that the token
     * identified by {@code tokenFingerprint} (a hash of the id_token) has been consumed
     * by the exchange flow.
     *
     * <p>Returns {@code true} when this is the first time the fingerprint has been seen
     * (the caller may proceed) and {@code false} when it has already been consumed and the
     * record has not yet expired (the caller must reject the request as a replay). The
     * record self-expires at {@code expiresAtMillis} — set to the token's own {@code exp},
     * after which the token is invalid anyway, so retaining the guard entry past that point
     * is unnecessary.
     *
     * <p>This is a best-effort check-then-set (the underlying cache offers no atomic
     * compare-and-set), so two requests racing within the same instant could both observe
     * "first use". That window is irrelevant to the threat this defends against — replay of
     * a <em>leaked</em> token minutes/hours later — which it closes.
     */
    boolean registerExchangeTokenUse(String tokenFingerprint, long expiresAtMillis);
}
