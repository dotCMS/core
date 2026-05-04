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
 * cache administrator. The {@link #CACHE_GROUP} routes through the same
 * cluster-aware cache provider as every other dotCMS cache, so in a clustered
 * deployment session-refs are automatically replicated across nodes — matching
 * the existing SAML session-replication footprint without any new plumbing.
 *
 * <p>Session-refs are {@value #ENTROPY_BYTES}-byte random strings encoded
 * URL-safe-base64 with the {@link DotAuthSessionCache#SESSION_REF_PREFIX}
 * prefix. That gives ~256 bits of entropy — well above the bar for a bearer
 * credential — while still fitting in a single HTTP header comfortably.
 */
public final class DotAuthSessionCacheImpl implements DotAuthSessionCache, Cachable {

    /** Named cache group for dotAuth sessions. */
    public static final String CACHE_GROUP = "DotAuthSessionCache";

    /** 32 bytes = 256 bits of entropy. */
    private static final int ENTROPY_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64  = Base64.getUrlEncoder().withoutPadding();

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
    public String getPrimaryGroup() {
        return CACHE_GROUP;
    }

    @Override
    public String[] getGroups() {
        return new String[]{CACHE_GROUP};
    }

    @Override
    public void clearCache() {
        invalidateAll();
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
