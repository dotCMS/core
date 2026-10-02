package com.dotcms.adminsite;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dotcms.business.SystemCache;
import com.dotmarketing.business.CacheLocator;
import com.github.benmanes.caffeine.cache.Cache;
import java.lang.reflect.Field;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Verifies that admin-site invalidation clears its calculated configuration
 * without touching the unrelated system-table cache.
 */
public class AdminSiteConfigCacheTest {

    /**
     * The Caffeine-backed implementation must clear its populated cache without
     * removing a system-cache entry that it never writes.
     */
    @Test
    public void invalidateCache_clearsCalculatedValuesWithoutTouchingSystemCache() throws Exception {
        final AdminSiteAPIImpl api = new AdminSiteAPIImpl();
        final Cache<String, Object> cache = calculatedCache(api);
        cache.put(AdminSiteAPI.ADMIN_SITE_URL, "https://admin.example.com");
        cache.put(AdminSiteAPI.ADMIN_SITE_ALLOW_BACKEND_LOGINS_ANY_SITE, true);
        assertEquals("The cache must be populated before invalidation", 2L, cache.estimatedSize());

        final SystemCache systemCache = mock(SystemCache.class);
        try (MockedStatic<CacheLocator> cacheLocator = mockStatic(CacheLocator.class)) {
            cacheLocator.when(CacheLocator::getSystemCache).thenReturn(systemCache);

            api.invalidateCache();

            assertEquals("Calculated configuration must be evicted", 0L, cache.estimatedSize());
            verifyNoInteractions(systemCache);
        }
    }

    /**
     * An implementation using the default hook has no calculated cache to clear
     * and must not remove entries from the system-table cache.
     */
    @Test
    public void defaultInvalidateCache_doesNotTouchSystemCache() {
        final AdminSiteAPI api = mock(AdminSiteAPI.class, CALLS_REAL_METHODS);
        final SystemCache systemCache = mock(SystemCache.class);
        try (MockedStatic<CacheLocator> cacheLocator = mockStatic(CacheLocator.class)) {
            cacheLocator.when(CacheLocator::getSystemCache).thenReturn(systemCache);

            api.invalidateCache();

            verifyNoInteractions(systemCache);
        }
    }

    /** Returns the private cache for seeding fixtures without Config or database lookups. */
    @SuppressWarnings("unchecked")
    private Cache<String, Object> calculatedCache(final AdminSiteAPIImpl api) throws Exception {
        final Field field = AdminSiteAPIImpl.class.getDeclaredField("adminSiteConfig");
        field.setAccessible(true);
        return (Cache<String, Object>) field.get(api);
    }
}
