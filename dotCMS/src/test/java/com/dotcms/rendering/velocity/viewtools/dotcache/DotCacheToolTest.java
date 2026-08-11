package com.dotcms.rendering.velocity.viewtools.dotcache;

import com.dotmarketing.business.cache.provider.MockCacheAdministrator;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that the {@link DotCacheTool} ViewTool works as expected.
 *
 * @author Will Ezell
 * @since Nov 14th, 2022
 */
public class DotCacheToolTest {

    /**
     * Upper bound for the cache to settle. Generous on purpose: it only caps how long a broken
     * assertion takes to report, it is not the expected wait.
     */
    private static final long SETTLE_TIMEOUT_SECONDS = 10L;

    private static DotCacheTool dotCacheTool;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dotCacheTool = new DotCacheTool(new MockCacheAdministrator());
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DotCacheTool#put(String, Object, int)}</li>
     *     <li><b>Given Scenario:</b> Adding a new entry to the DotCache with a TTL of 5 seconds, and verifying that it
     *     is present in the cache. After that, set the TTL to zero.</li>
     *     <li><b>Expected Result:</b> After setting the entry's TTL to zero, it must be flushed out of the cache and
     *     null must be returned.</li>
     * </ul>
     */
    @Test
    public void test_cache_TTL_0() {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now, 5);
        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        dotCacheTool.put(cacheKey, now, 0);
        Assert.assertNull(dotCacheTool.get(cacheKey));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DotCacheTool#put(String, Object, int)}</li>
     *     <li><b>Given Scenario:</b> Adding a new entry to the DotCache with a TTL of 2 seconds, and verifying that it
     *     is present in the cache. Then, wait for more than 2 seconds and check it again.</li>
     *     <li><b>Expected Result:</b> After waiting for more than 2 seconds, the entry must be flushed out of the
     *     cache and null must be returned.</li>
     * </ul>
     */
    @Test
    public void test_cache_TTL_2() {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now, 2);
        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        // the entry must expire on its own; polling rather than sleeping just past the TTL
        Awaitility.await()
                .atMost(SETTLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> dotCacheTool.get(cacheKey) == null);
    }

    
    @Test
    public void test_putDebounce() {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.putDebounce(cacheKey, now, 2);

        // not in cache yet, debouncing for 1 sec
        Assert.assertNull(dotCacheTool.get(cacheKey));

        // added once the debounce window elapses
        Awaitility.await()
                .atMost(SETTLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> now.equals(dotCacheTool.get(cacheKey)));
    }
    
    
    
    
    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DotCacheTool#clear()}</li>
     *     <li><b>Given Scenario:</b> Adding a new entry to the DotCache, and verifying that it is present in the cache.
     *     Then, clear the whole DotCache.</li>
     *     <li><b>Expected Result:</b> After clearing the whole cache, a null must be returned.</li>
     * </ul>
     */
    @Test
    public void test_cache_clear() {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now);

        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        dotCacheTool.clear();
        Assert.assertNull(dotCacheTool.get(cacheKey));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DotCacheTool#remove(String)}</li>
     *     <li><b>Given Scenario:</b> Adding a new entry to the DotCache, and verifying that it is present in the cache.
     *     Then, remove it from the DotCache by its key.</li>
     *     <li><b>Expected Result:</b> After explicitly removing it, a null must be returned.</li>
     * </ul>
     */
    @Test
    public void test_remove() {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        // NOTE: no wait needed. Only putDebounce() is debounced; put() writes through.
        dotCacheTool.put(cacheKey, now);
        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        dotCacheTool.remove(cacheKey);
        Assert.assertNull(dotCacheTool.get(cacheKey));
    }

}
