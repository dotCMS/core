package com.dotcms.adminsite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.business.SystemCache;
import com.dotcms.company.CompanyAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.Company;
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

    /** A production-configured URL must not depend on company/database availability. */
    @Test
    public void configuredUrlSkipsCompany() {
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class);
                MockedStatic<Config> config = mockStatic(Config.class)) {
            locator.when(APILocator::getCompanyAPI).thenReturn(companyAPI);
            config.when(() -> Config.getStringProperty(eq(AdminSiteAPI.ADMIN_SITE_URL),
                    nullable(String.class))).thenReturn("https://admin.example.com");

            assertEquals("https://admin.example.com", new AdminSiteAPIImpl().getAdminSiteUrl());
            verifyNoInteractions(companyAPI);
        }
    }

    /** A failed fallback lookup must be retried rather than cached as the built-in URL. */
    @Test
    public void failedLookupIsRetried() {
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final Company company = new Company("sample.company");
        company.setPortalURL("https://legacy.example.com");
        when(companyAPI.getDefaultCompany()).thenThrow(new DotRuntimeException("unavailable"))
                .thenReturn(company);

        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class);
                MockedStatic<Config> config = mockStatic(Config.class)) {
            locator.when(APILocator::getCompanyAPI).thenReturn(companyAPI);
            config.when(() -> Config.getStringProperty(eq(AdminSiteAPI.ADMIN_SITE_URL),
                    nullable(String.class))).thenAnswer(invocation -> invocation.getArgument(1));
            final AdminSiteAPIImpl api = new AdminSiteAPIImpl();

            assertThrows(DotRuntimeException.class, api::getAdminSiteUrl);
            assertEquals("https://legacy.example.com", api.getAdminSiteUrl());
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
