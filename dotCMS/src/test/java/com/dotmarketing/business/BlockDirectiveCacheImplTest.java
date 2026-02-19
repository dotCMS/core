package com.dotmarketing.business;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.business.cache.provider.MockCacheAdministrator;


public class BlockDirectiveCacheImplTest {



    private static BlockDirectiveCacheImpl blockCacheAdmin;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        blockCacheAdmin = new BlockDirectiveCacheImpl(new MockCacheAdministrator(), true);

    }



    @Test
    public void test_cache_TTL_0() {


        blockCacheAdmin.clearCache();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String, Serializable> cacheMap = Map.of(cacheKey, now);

        blockCacheAdmin.add(cacheKey, cacheMap, Duration.ofSeconds(5));
        Assert.assertTrue(blockCacheAdmin.get(cacheKey).size() > 0);

        blockCacheAdmin.add(cacheKey, cacheMap, Duration.ofSeconds(0));

        Assert.assertTrue(blockCacheAdmin.get(cacheKey).isEmpty());

    }


    @Test
    public void test_cache_TTL_2() throws InterruptedException {

        blockCacheAdmin.clearCache();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String, Serializable> cacheMap = Map.of(cacheKey, now);

        blockCacheAdmin.add(cacheKey, cacheMap, Duration.ofSeconds(2));
        Assert.assertTrue(blockCacheAdmin.get(cacheKey).size() > 0);

        Thread.sleep(2500);

        Assert.assertTrue(blockCacheAdmin.get(cacheKey).isEmpty());

    }


    @Test
    public void test_cache_clear() throws InterruptedException {

        blockCacheAdmin.clearCache();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String, Serializable> cacheMap = Map.of(cacheKey, now);

        blockCacheAdmin.add(cacheKey, cacheMap, Duration.ofSeconds(Integer.MAX_VALUE));
        Assert.assertTrue(blockCacheAdmin.get(cacheKey).size() > 0);

        blockCacheAdmin.clearCache();
        Assert.assertTrue(blockCacheAdmin.get(cacheKey).isEmpty());

    }

    @Test
    public void test_remove() throws InterruptedException {

        blockCacheAdmin.clearCache();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String, Serializable> cacheMap = Map.of(cacheKey, now);

        blockCacheAdmin.add(cacheKey, cacheMap, Duration.ofSeconds(Integer.MAX_VALUE));
        Assert.assertTrue(blockCacheAdmin.get(cacheKey).size() > 0);

        blockCacheAdmin.remove(cacheKey);
        Assert.assertTrue(blockCacheAdmin.get(cacheKey).isEmpty());

    }



}
