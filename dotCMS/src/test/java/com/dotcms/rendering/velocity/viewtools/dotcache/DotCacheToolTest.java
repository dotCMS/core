package com.dotcms.rendering.velocity.viewtools.dotcache;

import java.io.Serializable;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.business.cache.provider.MockCacheAdministrator;

public class DotCacheToolTest {


    private static DotCacheTool dotCacheTool;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        dotCacheTool = new DotCacheTool(new MockCacheAdministrator());

    }


    @Test
    public void test_cache_TTL_0() {


        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();

        dotCacheTool.put(cacheKey, now, 5);
        Assert.assertTrue(dotCacheTool.get(cacheKey).equals(now));

        dotCacheTool.put(cacheKey, now, 0);

        Assert.assertTrue(dotCacheTool.get(cacheKey)==null);

    }


    @Test
    public void test_cache_TTL_2() throws InterruptedException {

        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();


        dotCacheTool.put(cacheKey, now, 2);
        Assert.assertTrue(dotCacheTool.get(cacheKey).equals(now));

        Thread.sleep(2500);

        Assert.assertTrue(dotCacheTool.get(cacheKey)==null);

    }


    @Test
    public void test_cache_clear() throws InterruptedException {

        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String, Serializable> cacheMap = Map.of(cacheKey, now);

        dotCacheTool.put(cacheKey, now);
        Assert.assertTrue(dotCacheTool.get(cacheKey).equals(now));

        dotCacheTool.clear();
        Assert.assertTrue(dotCacheTool.get(cacheKey)==null);

    }

    @Test
    public void test_remove() throws InterruptedException {

        dotCacheTool.clear();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String, Serializable> cacheMap = Map.of(cacheKey, now);

        dotCacheTool.put(cacheKey, now);
        Assert.assertTrue(dotCacheTool.get(cacheKey).equals(now));

        dotCacheTool.remove(cacheKey);
        Assert.assertTrue(dotCacheTool.get(cacheKey)==null);

    }

}
