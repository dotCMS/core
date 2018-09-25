package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache.DotJSONCacheKey;
import com.dotcms.cache.DotJSONCacheAddTestCase.Builder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.NoSuchElementException;

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertNull;

@RunWith(DataProviderRunner.class)
public class DotJSONCacheTest {

    private static final String VALID_KEY = "validKey";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] addTestCases() {
        final DotJSON<String, String> dotJSONZeroTTL = new DotJSON<>();
        final DotJSON<String, Integer> dotJSONPositiveTTL = new DotJSON<>();
        dotJSONPositiveTTL.put(DotJSON.CACHE_TTL_KEY, 1000);

        return new DotJSONCacheAddTestCase[] {
                new Builder().cacheKey(null).shouldCache(false).build(),
                new Builder().cacheKey(VALID_KEY).dotJSON(dotJSONZeroTTL).shouldCache(false).build(),
                new Builder().cacheKey(VALID_KEY).dotJSON(dotJSONPositiveTTL).shouldCache(true).build()
        };
    }

    @DataProvider
    public static Object[] getTestCases() {
        final DotJSON<String, Integer> dotJSON1000TTL = new DotJSON<>();
        dotJSON1000TTL.put(DotJSON.CACHE_TTL_KEY, 500);

        return new DotJSONCacheGetTestCase[] {
                new DotJSONCacheGetTestCase.Builder().dotJSONToAdd(dotJSON1000TTL).cacheKeyToAdd(VALID_KEY)
                        .cacheKeyToGet(VALID_KEY).waitTime(0).dotJSONToExpect(dotJSON1000TTL).build(),
                new DotJSONCacheGetTestCase.Builder().dotJSONToAdd(dotJSON1000TTL).cacheKeyToAdd(VALID_KEY)
                .cacheKeyToGet(VALID_KEY)
                        .waitTime(700).dotJSONToExpect(null).build(),
                new DotJSONCacheGetTestCase.Builder().dotJSONToAdd(dotJSON1000TTL).cacheKeyToAdd(VALID_KEY)
                        .cacheKeyToGet("INVALID-KEY").waitTime(0).dotJSONToExpect(null).build()

        };
    }

    @Test
    @UseDataProvider("addTestCases")
    public void testAdd(final DotJSONCacheAddTestCase testCase) {
        final DotJSONCache cache = CacheLocator.getDotJSONCache();
        final DotJSONCacheKey cacheKey = Mockito.mock(DotJSONCacheKey.class);
        Mockito.when(cacheKey.getKey()).thenReturn(testCase.getCacheKey());

        try {
            cache.add(cacheKey, testCase.getDotJSON());
        } catch(Exception e) {
            // not doing anything if exception
        }

        assertEquals(testCase.shouldCache(), cache.get(cacheKey).isPresent());
    }

    @Test
    @UseDataProvider("getTestCases")
    public void testGet(final DotJSONCacheGetTestCase testCase) {
        final DotJSONCache cache = CacheLocator.getDotJSONCache();
        final DotJSONCacheKey cacheKeyToAdd = Mockito.mock(DotJSONCacheKey.class);
        Mockito.when(cacheKeyToAdd.getKey()).thenReturn(testCase.cacheKeyToAdd());

        final DotJSONCacheKey cacheKeyToGet = Mockito.mock(DotJSONCacheKey.class);
        Mockito.when(cacheKeyToGet.getKey()).thenReturn(testCase.cacheKeyToGet());

        cache.add(cacheKeyToAdd, testCase.dotJSONToAdd());

        if(testCase.waitTime()>0) {
            try {
                Thread.sleep(testCase.waitTime());
            } catch (InterruptedException e) {
                Logger.error(this, "Could not sleep during testing...too much coffee");
            }
        }

        DotJSON<String, Integer> dotJSONFromCache;

        try {
            dotJSONFromCache = cache.get(cacheKeyToGet).get();
        } catch (NoSuchElementException e) {
            dotJSONFromCache = null;
        }

        if(UtilMethods.isSet(testCase.dotJSONToExpect())) {
            assertEquals(testCase.dotJSONToExpect(), dotJSONFromCache);
        } else {
            assertNull("Expected null DotJSON from cache", dotJSONFromCache);
        }

    }
}
