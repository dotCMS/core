package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CustomAttributeCacheImplTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link CustomAttributeCacheImpl#put(String, Map)}, {@link CustomAttributeCacheImpl#get(String)}
     *                 and {@link CustomAttributeCacheImpl#clearCache()}
     * When: Put a customAttributesMatch inside the cache and then try to get it
     * Should: return the right customAttributeMatch
     * But after called the clearCache method should return null
     */
    @Test
    public void putAndClear(){
        final CustomAttributeCacheImpl cache = new CustomAttributeCacheImpl();
        final Map<String, String> customAttributesMatch = Map.of("title", "custom_1", "name", "custom_2");

        cache.put(EventType.PAGE_VIEW.getName(), customAttributesMatch);

        final Map<String, String> getFromCache = cache.get(EventType.PAGE_VIEW.getName());

        assertEquals(customAttributesMatch, getFromCache);

        cache.clearCache();
        final Map<String, String> getFromCache2 = cache.get(EventType.PAGE_VIEW.getName());
        assertNull( getFromCache2);
    }
}
