package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityListStringView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.rest.exception.BadRequestException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for the Cache Management REST endpoints in {@link CacheResource}.
 * Tests verify endpoint behavior with real cache infrastructure.
 *
 * @author hassandotcms
 */
public class CacheResourceIntegrationTest {

    private static CacheResource cacheResource;
    private static HttpServletResponse mockResponse;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminUser = TestUserUtils.getAdminUser();

        final Role backendRole = TestUserUtils.getBackendRole();
        APILocator.getRoleAPI().addRoleToUser(backendRole, adminUser);

        cacheResource = new CacheResource();
        mockResponse = new MockHttpResponse();
    }

    // =========================================================================
    // GET /api/v1/caches — List Cache Regions
    // =========================================================================

    /**
     * Given: Authenticated admin user with maintenance portlet access
     * When: listRegions is called
     * Then: Returns non-empty sorted list of cache region names
     */
    @Test
    public void test_listRegions_returns_sorted_list() {

        final ResponseEntityListStringView result =
                cacheResource.listRegions(mockAuthenticatedRequest(), mockResponse);

        assertNotNull(result);
        final List<String> regions = result.getEntity();
        assertNotNull(regions);
        assertFalse("Regions list should not be empty", regions.isEmpty());

        // Verify sorting
        for (int i = 1; i < regions.size(); i++) {
            assertTrue(
                    "Regions should be alphabetically sorted, but found '"
                            + regions.get(i - 1) + "' before '" + regions.get(i) + "'",
                    regions.get(i - 1).compareTo(regions.get(i)) <= 0);
        }

        // Verify well-known regions are present
        assertTrue("Should contain 'Permission' region", regions.contains("Permission"));
        assertTrue("Should contain 'System' region", regions.contains("System"));
        assertTrue("Should contain 'Contentlet' region", regions.contains("Contentlet"));
    }

    /**
     * Given: Authenticated admin user
     * When: listRegions is called
     * Then: Region count matches CacheLocator.getCacheIndexes() length
     */
    @Test
    public void test_listRegions_count_matches_cache_indexes() {

        final ResponseEntityListStringView result =
                cacheResource.listRegions(mockAuthenticatedRequest(), mockResponse);

        final Object[] cacheIndexes = CacheLocator.getCacheIndexes();
        assertEquals("Region count should match CacheIndex enum values",
                cacheIndexes.length, result.getEntity().size());
    }

    // =========================================================================
    // GET /api/v1/caches/stats — Cache Statistics
    // =========================================================================

    /**
     * Given: Authenticated admin user
     * When: getCacheStats is called
     * Then: Returns valid JVM memory stats with all fields positive
     */
    @Test
    public void test_getCacheStats_returns_valid_memory() {

        final ResponseEntityCacheStatsView result =
                cacheResource.getCacheStats(mockAuthenticatedRequest(), mockResponse);

        assertNotNull(result);
        final CacheStatsView stats = result.getEntity();
        assertNotNull(stats);

        assertNotNull("clusterId should be present", stats.clusterId());
        assertFalse("clusterId should not be empty", stats.clusterId().isEmpty());
        assertNotNull("serverId should be present", stats.serverId());
        assertFalse("serverId should not be empty", stats.serverId().isEmpty());

        final JvmMemoryView memory = stats.memory();
        assertNotNull(memory);
        assertTrue("maxMemory should be positive", memory.maxMemory() > 0);
        assertTrue("allocatedMemory should be positive", memory.allocatedMemory() > 0);
        assertTrue("usedMemory should be positive", memory.usedMemory() > 0);
        assertTrue("freeMemory should be non-negative", memory.freeMemory() >= 0);
        assertEquals("freeMemory should equal maxMemory minus usedMemory",
                memory.maxMemory() - memory.usedMemory(), memory.freeMemory());
    }

    /**
     * Given: Authenticated admin user
     * When: getCacheStats is called
     * Then: Returns at least one provider with columns and stats
     */
    @Test
    public void test_getCacheStats_returns_provider_stats() {

        final ResponseEntityCacheStatsView result =
                cacheResource.getCacheStats(mockAuthenticatedRequest(), mockResponse);

        final List<CacheProviderStatsView> providers = result.getEntity().providers();
        assertNotNull(providers);
        assertFalse("Should have at least one cache provider", providers.isEmpty());

        final CacheProviderStatsView firstProvider = providers.get(0);
        assertNotNull("Provider name should not be null", firstProvider.providerName());
        assertFalse("Provider should report columns", firstProvider.columns().isEmpty());
        assertFalse("Provider should have stats entries", firstProvider.stats().isEmpty());

        // Verify stat rows have the same keys as columns
        final Map<String, String> firstRow = firstProvider.stats().get(0);
        for (final String column : firstProvider.columns()) {
            assertTrue("Stat row should contain column '" + column + "'",
                    firstRow.containsKey(column));
        }
    }

    // =========================================================================
    // DELETE /api/v1/caches/region/{regionName} — Flush Region
    // =========================================================================

    /**
     * Given: Valid cache region name "Permission"
     * When: flushRegion is called
     * Then: Returns success message confirming the region was flushed
     */
    @Test
    public void test_flushRegion_valid_returns_success() {

        final ResponseEntityStringView result =
                cacheResource.flushRegion(
                        mockAuthenticatedRequest(), mockResponse, "Permission");

        assertNotNull(result);
        assertEquals("Flushed Permission", result.getEntity());
    }

    /**
     * Given: regionName is "all"
     * When: flushRegion is called
     * Then: Returns success message confirming all caches were flushed
     */
    @Test
    public void test_flushRegion_all_returns_success() {

        final ResponseEntityStringView result =
                cacheResource.flushRegion(
                        mockAuthenticatedRequest(), mockResponse, "all");

        assertNotNull(result);
        assertEquals("Flushed all caches", result.getEntity());
    }

    /**
     * Given: regionName is "ALL" (uppercase)
     * When: flushRegion is called
     * Then: Case-insensitive match flushes all caches
     */
    @Test
    public void test_flushRegion_all_case_insensitive() {

        final ResponseEntityStringView result =
                cacheResource.flushRegion(
                        mockAuthenticatedRequest(), mockResponse, "ALL");

        assertNotNull(result);
        assertEquals("Flushed all caches", result.getEntity());
    }

    /**
     * Given: Region name in wrong case ("permission" instead of "Permission")
     * When: flushRegion is called
     * Then: Resolves case-insensitively and returns canonical name in response
     */
    @Test
    public void test_flushRegion_case_insensitive_resolves_to_canonical() {

        final ResponseEntityStringView result =
                cacheResource.flushRegion(
                        mockAuthenticatedRequest(), mockResponse, "permission");

        assertNotNull(result);
        assertEquals("Flushed Permission", result.getEntity());
    }

    /**
     * Given: Unknown/invalid region name
     * When: flushRegion is called
     * Then: BadRequestException is thrown
     */
    @Test(expected = BadRequestException.class)
    public void test_flushRegion_unknown_returns_400() {

        cacheResource.flushRegion(
                mockAuthenticatedRequest(), mockResponse, "NonExistentRegion123");
    }

    /**
     * Given: Valid region name "System"
     * When: flushRegion is called
     * Then: Flushes successfully (System region also triggers PushPublishing reload internally)
     */
    @Test
    public void test_flushRegion_system_returns_success() {

        final ResponseEntityStringView result =
                cacheResource.flushRegion(
                        mockAuthenticatedRequest(), mockResponse, "System");

        assertNotNull(result);
        assertEquals("Flushed System", result.getEntity());
    }

    // =========================================================================
    // Existing flushAll() — Modified endpoint
    // =========================================================================

    /**
     * Given: Authenticated admin user calls the existing flush-all endpoint
     * When: flushAll is called
     * Then: Returns success (verifies the helper integration works)
     */
    @Test
    public void test_flushAll_existing_endpoint_returns_success() {

        final javax.ws.rs.core.Response result =
                cacheResource.flushAll(
                        mockAuthenticatedRequest(), mockResponse, "any-provider");

        assertNotNull(result);
        assertEquals(200, result.getStatus());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private HttpServletRequest mockAuthenticatedRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/api/v1/caches")
                                        .request())
                                .request())
                        .request());

        request.setAttribute(WebKeys.USER, adminUser);
        return request;
    }
}
