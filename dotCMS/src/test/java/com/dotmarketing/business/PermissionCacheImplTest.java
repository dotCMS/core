package com.dotmarketing.business;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.cache.provider.MockCacheAdministrator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PermissionCacheImpl}.
 *
 * Guards against regressions of https://github.com/dotCMS/private-issues/issues/567
 * where {@code clearCache()} did not flush {@code shortLivedGroup}, so the
 * admin-UI "Flush Permission Cache" button left cached permission decisions
 * in place and role revocation did not propagate.
 *
 * Exercises the cache administrator directly rather than going through
 * {@code doesUserHavePermission}/{@code putUserHavePermission}, because the
 * latter call {@code DbConnectionFactory.inTransaction()} which requires a
 * configured data source — out of scope for a pure unit test.
 */
public class PermissionCacheImplTest {

    private static final String PRIMARY_GROUP = "PermissionCache";
    private static final String SHORT_LIVED_GROUP = "PermissionShortLived";

    private MockCacheAdministrator mockCache;
    private PermissionCacheImpl permissionCache;

    @Before
    public void setUp() {
        mockCache = new MockCacheAdministrator();
        permissionCache = new PermissionCacheImpl(mockCache);
    }

    @Test
    public void clearCache_flushesPrimaryGroup() throws Exception {
        permissionCache.addToPermissionCache("k1", List.of(new Permission()));
        assertNotNull("Primary entry should be present before clearCache",
                mockCache.get(PRIMARY_GROUP + "k1", PRIMARY_GROUP));

        permissionCache.clearCache();

        assertNull("Primary entry should be gone after clearCache",
                mockCache.get(PRIMARY_GROUP + "k1", PRIMARY_GROUP));
    }

    @Test
    public void clearCache_flushesShortLivedGroup() throws Exception {
        // Seed shortLivedGroup directly to avoid DbConnectionFactory init via
        // putUserHavePermission -> shortLivedKey.
        mockCache.put("short-k1", Boolean.TRUE, SHORT_LIVED_GROUP);
        assertNotNull("Short-lived entry should be present before clearCache",
                mockCache.get("short-k1", SHORT_LIVED_GROUP));

        permissionCache.clearCache();

        assertNull("Short-lived entry must be gone after clearCache — this is the"
                        + " fix for private-issues#567",
                mockCache.get("short-k1", SHORT_LIVED_GROUP));
    }

    @Test
    public void flushShortTermCache_leavesPrimaryGroupUntouched() throws Exception {
        permissionCache.addToPermissionCache("primary-key", List.of(new Permission()));
        mockCache.put("short-k2", Boolean.TRUE, SHORT_LIVED_GROUP);

        permissionCache.flushShortTermCache();

        assertNotNull("primaryGroup must be untouched by flushShortTermCache()",
                mockCache.get(PRIMARY_GROUP + "primary-key", PRIMARY_GROUP));
        assertNull("shortLivedGroup must be empty after flushShortTermCache()",
                mockCache.get("short-k2", SHORT_LIVED_GROUP));
    }
}
