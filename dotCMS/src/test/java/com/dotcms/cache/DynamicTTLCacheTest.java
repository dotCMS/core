package com.dotcms.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class DynamicTTLCacheTest {


    private DynamicTTLCache<String, String> cache;

    @BeforeEach
    public void setUp() {
        cache = new DynamicTTLCache<>(100, 5000); // maxCapacity = 100, defaultTTLInMillis = 5000
    }

    @Test
    public void testPutAndGetIfPresent() {
        cache.put("key1", "value1", 2000);
        assertEquals("value1", cache.getIfPresent("key1"));
    }

    @Test
    public void testGetIfPresentAfterTTLExpires() throws InterruptedException {
        cache.put("key2", "value2", 1000);
        assertNotNull(cache.getIfPresent("key2"));
        TimeUnit.MILLISECONDS.sleep(1500);
        assertNull(cache.getIfPresent("key2"));
    }

    @Test
    public void testInvalidate() {
        cache.put("key3", "value3", 2000);
        cache.invalidate("key3");
        assertNull(cache.getIfPresent("key3"));
    }

    @Test
    public void testInvalidateAll() {
        cache.put("key4", "value4", 2000);
        cache.put("key5", "value5", 2000);
        cache.invalidateAll();
        assertNull(cache.getIfPresent("key4"));
        assertNull(cache.getIfPresent("key5"));
    }

    @Test
    public void testEstimatedSize() {
        cache.invalidateAll();
        cache.put("key6", "value6", 2000);
        cache.put("key7", "value7", 2000);
        assertEquals(2, cache.estimatedSize());
    }

    @Test
    public void testStats() {
        cache.put("key8", "value8", 2000);
        assertNotNull(cache.getIfPresent("key8"));

        assertNotNull(cache.stats());
    }

    @Test
    public void testCacheExpiry()throws InterruptedException {
        cache.put("key8", "value8");
        TimeUnit.MILLISECONDS.sleep(6000);
        assertNull(cache.getIfPresent("key8"));

    }


    @Test
    public void testCacheTTLGreaterThanObjectTTL()throws InterruptedException {
        // cache has a 5000ms timeout
        cache.put("10Seconds", "10Seconds", 10000);
        TimeUnit.MILLISECONDS.sleep(7000);
        assertNotNull(cache.getIfPresent("10Seconds"));
        TimeUnit.MILLISECONDS.sleep(4000);
        assertNull(cache.getIfPresent("10Seconds"));
    }




}
