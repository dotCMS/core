package com.dotcms.auth.dotAuth.session;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.UtilMethods;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Default {@link DotAuthSessionCache} implementation, backed by the dotCMS
 * cache administrator.
 *
 * <p><strong>Cluster scope:</strong> the default cache transport broadcasts
 * invalidations only — values are never replicated between nodes. A session-ref
 * minted on one node is therefore NOT visible on its peers, and the replay
 * guard is enforced per node. Clustered deployments must either route headless
 * API traffic with session affinity (sticky on the {@code Authorization}
 * header / source) or configure a distributed cache provider (e.g. Redis) for
 * {@link #CACHE_GROUP} and {@link #REPLAY_CACHE_GROUP}.
 *
 * <p>Session-refs are {@value #ENTROPY_BYTES}-byte random strings encoded
 * URL-safe-base64 with the {@link DotAuthSessionCache#SESSION_REF_PREFIX}
 * prefix. That gives ~256 bits of entropy — well above the bar for a bearer
 * credential — while still fitting in a single HTTP header comfortably.
 */
public final class DotAuthSessionCacheImpl implements DotAuthSessionCache, Cachable {

    /** Named cache group for dotAuth sessions. */
    public static final String CACHE_GROUP = "DotAuthSessionCache";

    /** Named cache group for the exchanged-id_token one-time-use (replay) guard. */
    public static final String REPLAY_CACHE_GROUP = "DotAuthTokenReplayCache";

    /** 32 bytes = 256 bits of entropy. */
    private static final int ENTROPY_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64  = Base64.getUrlEncoder().withoutPadding();
    private static final Object REPLAY_LOCK  = new Object();

    private static final class SingletonHolder {
        private static final DotAuthSessionCacheImpl INSTANCE = new DotAuthSessionCacheImpl();
    }

    public static DotAuthSessionCacheImpl getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private DotAuthSessionCacheImpl() { }

    @Override
    public String create(final String userId, final long lifetimeMillis) {
        final long now       = System.currentTimeMillis();
        final long expiresAt = now + Math.max(0L, lifetimeMillis);
        final String ref     = SESSION_REF_PREFIX + B64.encodeToString(randomBytes());
        cache().put(ref, new DotAuthSession(userId, now, expiresAt), CACHE_GROUP);
        return ref;
    }

    @Override
    public Optional<DotAuthSession> get(final String sessionRef) {
        if (!UtilMethods.isSet(sessionRef) || !sessionRef.startsWith(SESSION_REF_PREFIX)) {
            return Optional.empty();
        }
        final Object raw = cache().getNoThrow(sessionRef, CACHE_GROUP);
        if (!(raw instanceof DotAuthSession)) {
            return Optional.empty();
        }
        final DotAuthSession session = (DotAuthSession) raw;
        if (session.isExpired()) {
            // Lazy eviction — don't trust provider-side TTL alone; enforce our own absolute cap.
            cache().remove(sessionRef, CACHE_GROUP);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public void invalidate(final String sessionRef) {
        if (!UtilMethods.isSet(sessionRef) || !sessionRef.startsWith(SESSION_REF_PREFIX)) {
            return;
        }
        cache().remove(sessionRef, CACHE_GROUP);
    }

    @Override
    public void invalidateAll() {
        cache().flushGroup(CACHE_GROUP);
    }

    @Override
    public boolean registerExchangeTokenUse(final String tokenFingerprint, final long expiresAtMillis) {
        if (!UtilMethods.isSet(tokenFingerprint)) {
            // No fingerprint to track — let the caller proceed. Callers always derive one,
            // so this only guards against a programming error rather than gating real traffic.
            return true;
        }
        // The cache administrator offers no atomic putIfAbsent, so the check-then-put must be
        // a critical section or two concurrent exchanges of the same id_token both pass the
        // one-time-use guard. Single lock is fine: this runs once per token exchange.
        synchronized (REPLAY_LOCK) {
            final Object raw = cache().getNoThrow(tokenFingerprint, REPLAY_CACHE_GROUP);
            if (raw instanceof Long && System.currentTimeMillis() < (Long) raw) {
                // Already consumed and the token has not yet expired -> replay.
                return false;
            }
            cache().put(tokenFingerprint, Long.valueOf(expiresAtMillis), REPLAY_CACHE_GROUP);
            return true;
        }
    }

    @Override
    public String getPrimaryGroup() {
        return CACHE_GROUP;
    }

    @Override
    public String[] getGroups() {
        return new String[]{CACHE_GROUP, REPLAY_CACHE_GROUP};
    }

    @Override
    public void clearCache() {
        cache().flushGroup(CACHE_GROUP);
        cache().flushGroup(REPLAY_CACHE_GROUP);
    }

    private static DotCacheAdministrator cache() {
        return CacheLocator.getCacheAdministrator();
    }

    private static byte[] randomBytes() {
        final byte[] buf = new byte[ENTROPY_BYTES];
        RANDOM.nextBytes(buf);
        return buf;
    }
}
