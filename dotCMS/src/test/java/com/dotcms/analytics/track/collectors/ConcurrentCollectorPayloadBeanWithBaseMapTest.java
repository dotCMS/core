package com.dotcms.analytics.track.collectors;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Test for the ConcurrentCollectorPayloadBeanWithBaseMap
 * @author jsanca
 */
public class ConcurrentCollectorPayloadBeanWithBaseMapTest {

    /**
     * Method to test: {@link ConcurrentCollectorPayloadBeanWithBaseMap}
     * Given Scenario: Create a ConcurrentCollectorPayloadBeanWithBaseMap and a base map
     * ExpectedResult: The base map values will try to be overriden, but it should not be, taking the base map
     * into the Collector Payload as a source of truth
     */
    @Test
    public void test_base_map_do_not_get_overriden() throws IOException {

        final CollectorPayloadBean concurrentCollectorPayloadBeanWithBaseMap =
                new ConcurrentCollectorPayloadBeanWithBaseMap(Map.of("key1", "value1", "key2", "value2"));

        concurrentCollectorPayloadBeanWithBaseMap.put("key1", "value1.1"); // should not
        concurrentCollectorPayloadBeanWithBaseMap.put("key2", "value2.1"); // should not
        concurrentCollectorPayloadBeanWithBaseMap.put("key3", "value3"); // should

        concurrentCollectorPayloadBeanWithBaseMap.add(new ConcurrentCollectorPayloadBean(Map.of("key1", "value1.2",
                "key2", "value2.2", "key3", "value3.1", "key4", "value4")));

        final Map<String, Serializable> finalMap = concurrentCollectorPayloadBeanWithBaseMap.toMap();

        Assert.assertNotNull("The map should be not null", finalMap);
        Assert.assertFalse("The map should be not empty", finalMap.isEmpty());
        Assert.assertEquals(    "key1 should not change, should be always value1","value1", finalMap.get("key1"));
        Assert.assertEquals(    "key2 should not change, should be always value2","value2", finalMap.get("key2"));
        Assert.assertEquals(    "key3 should change, should be value3.1","value3.1", finalMap.get("key3"));
        Assert.assertEquals(    "key4 should change, should be value4","value4", finalMap.get("key4"));
    }
}
