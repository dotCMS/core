package com.dotcms.rendering.velocity.viewtools.dotcache;

import com.dotmarketing.business.cache.provider.MockCacheAdministrator;
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
     *
     * @throws InterruptedException Highly unlikely error related to the {@code Thread.sleep()} call, which is necessary
     *                              because of the Debouncer being applied to the {@code DotCacheTool.put()} methods.
     */
    @Test
    public void test_cache_TTL_0() throws InterruptedException {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now, 5);
        Thread.sleep(1200);
        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        dotCacheTool.put(cacheKey, now, 0);
        Thread.sleep(1200);
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
     *
     * @throws InterruptedException Highly unlikely error related to the {@code Thread.sleep()} call, which is necessary
     *                              because of the Debouncer being applied to the {@code DotCacheTool.put()} methods.
     */
    @Test
    public void test_cache_TTL_2() throws InterruptedException {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now, 2);
        Thread.sleep(1200);
        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        Thread.sleep(2500);
        Assert.assertNull(dotCacheTool.get(cacheKey));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DotCacheTool#clear()}</li>
     *     <li><b>Given Scenario:</b> Adding a new entry to the DotCache, and verifying that it is present in the cache.
     *     Then, clear the whole DotCache.</li>
     *     <li><b>Expected Result:</b> After clearing the whole cache, a null must be returned.</li>
     * </ul>
     *
     * @throws InterruptedException Highly unlikely error related to the {@code Thread.sleep()} call, which is necessary
     *                              because of the Debouncer being applied to the {@code DotCacheTool.put()} methods.
     */
    @Test
    public void test_cache_clear() throws InterruptedException {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now);
        Thread.sleep(1200);
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
     *
     * @throws InterruptedException Highly unlikely error related to the {@code Thread.sleep()} call, which is necessary
     *                              because of the Debouncer being applied to the {@code DotCacheTool.put()} methods.
     */
    @Test
    public void test_remove() throws InterruptedException {
        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now);
        Thread.sleep(1200);
        Assert.assertEquals(dotCacheTool.get(cacheKey), now);

        dotCacheTool.remove(cacheKey);
        Assert.assertNull(dotCacheTool.get(cacheKey));
    }

}
