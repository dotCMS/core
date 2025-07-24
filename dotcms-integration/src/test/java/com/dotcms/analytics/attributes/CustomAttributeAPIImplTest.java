package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CustomAttributeAPIImplTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void cleanTable() throws Exception {
        new DotConnect().setSQL("DELETE FROM analytic_custom_attributes").loadResult();
        CacheLocator.getAnalyticsCustomAttributeCache().clearCache();
    }


    /**
     * Method to test: constructor of {@link CustomAttributeAPIImpl}
     * When: A instance is created
     * Should: load all the custom attributes match from the database
     */
    @Test
    public void loadAllCustomAttributesMatch() throws DotDataException {
        final CustomAttributeCache analyticsCustomAttributeCache = CacheLocator.getAnalyticsCustomAttributeCache();
        assertNull(analyticsCustomAttributeCache.get(EventType.PAGE_VIEW.getName()));

        final CustomAttributeFactory analyticsCustomAttributeFactory = FactoryLocator.getAnalyticsCustomAttributeFactory();
        final Map<String, String> customAttributesMatch = Map.of("name", "custom_1", "title", "custom_2");

        analyticsCustomAttributeFactory.save(EventType.PAGE_VIEW.getName(), customAttributesMatch);

        final CustomAttributeAPI customAttributeAPI = new CustomAttributeAPIImpl();

        final Map<String, String> customAttributeFromCache = analyticsCustomAttributeCache.get(EventType.PAGE_VIEW.getName());
        assertEquals(customAttributesMatch, customAttributeFromCache);
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)}
     * When: called the method with a set of custom attributes match
     * Should: save them into the database
     */
    @Test
    public void checkCustomPayloadValidationIsSaved() throws DotDataException, IOException {

        final String eventName = "Test_Event_" + System.currentTimeMillis();
        final CustomAttributeCache analyticsCustomAttributeCache = CacheLocator.getAnalyticsCustomAttributeCache();
        assertNull(analyticsCustomAttributeCache.get(eventName));

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final List<Map<String, Object>> dataBaseAttributesBefore = get(eventName);
        assertTrue(dataBaseAttributesBefore.isEmpty());

        final Map<String, Object> customAttributesPayload= Map.of("name", "name_value",
                "title", "title_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final List<Map<String, Object>> dataBaseAttributesAfter = get(eventName);
        assertEquals(1, dataBaseAttributesAfter.size());

        final Map<String, Object> customAttributes =
                JsonUtil.getJsonFromString(dataBaseAttributesAfter.get(0).get("custom_attribute").toString());

        assertEquals(2, customAttributes.size());
        assertTrue(customAttributes.containsKey("name"));
        assertTrue(customAttributes.containsKey("title"));

        assertTrue(customAttributes.containsValue("custom_1"));
        assertTrue(customAttributes.containsValue("custom_2"));

        final Map<String, String> customAttributeFromCache = analyticsCustomAttributeCache.get(eventName);

        assertEquals(2, customAttributes.size());
        assertTrue(customAttributes.containsKey("name"));
        assertTrue(customAttributes.containsKey("title"));

        assertTrue(customAttributes.containsValue("custom_1"));
        assertTrue(customAttributes.containsValue("custom_2"));
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)}
     * When:
     * - Called the method with a set of custom attributes and a EventType equals to 'A', The cache must keep empty after it
     * - Called the method again with more custom attributes but for EventType equals to 'B', The cache must keep empty after it
     * - Called again the method with the same custom attributes before for the A event, now the cache must be loaded for A and B
     *
     * Should: The cache be loaded with both A and B
     */
    @Test
    public void calledTwice() throws DotDataException {
        final CustomAttributeCache analyticsCustomAttributeCache = CacheLocator.getAnalyticsCustomAttributeCache();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesMatchA = Map.of("A1", "value_A1", "A2", "value_A2");
        customAttributeAPI.checkCustomPayloadValidation("A", customAttributesMatchA);
        assertNull(analyticsCustomAttributeCache.get("A"));

        final Map<String, Object> customAttributesMatchB = Map.of("B1", "value_B1", "B2", "value_B2");
        customAttributeAPI.checkCustomPayloadValidation("B", customAttributesMatchB);
        assertNull(analyticsCustomAttributeCache.get("B"));

        customAttributeAPI.checkCustomPayloadValidation("A", customAttributesMatchA);

        final Map<String, String> customAttributeFromCacheB = analyticsCustomAttributeCache.get("B");
        assertEquals(2, customAttributeFromCacheB.size());
        assertTrue(customAttributeFromCacheB.containsKey("B1"));
        assertTrue(customAttributeFromCacheB.containsKey("B2"));

        assertTrue(customAttributeFromCacheB.containsValue("custom_1"));
        assertTrue(customAttributeFromCacheB.containsValue("custom_2"));

        final Map<String, String> customAttributeFromCacheA_2 = analyticsCustomAttributeCache.get("A");
        assertEquals(2, customAttributeFromCacheA_2.size());
        assertTrue(customAttributeFromCacheA_2.containsKey("A1"));
        assertTrue(customAttributeFromCacheA_2.containsKey("A2"));

        assertTrue(customAttributeFromCacheA_2.containsValue("custom_1"));
        assertTrue(customAttributeFromCacheA_2.containsValue("custom_2"));
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)}
     * When: called the method with a set of custom attributes match that is greater than the max limit
     * Should: throw a {@link MaxCustomAttributesReachedException} is thrown
     */
    @Test
    public void checkCustomPayloadValidationMaxLimitReached() throws DotDataException {
        final Map<String, Object> customAttributesPayload = new HashMap<>();

        for (int i = 0; i < 51; i++) {
            customAttributesPayload.put("A" + i, "value_" + i);
        }

        try {
            APILocator.getAnalyticsCustomAttribute().checkCustomPayloadValidation("test", customAttributesPayload);
            throw new DotDataException("Max limit reached Expected");
        } catch (MaxCustomAttributesReachedException e) {
            assertTrue("Totally expected", true);
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)}
     * When: called the method twice the fist one with 2 custom attributes: 'A' and 'B', the second time with
     * 'B' and 'C', both with the same event_type
     * Should: Finish with 3 custom attributes register: 'A', 'B' and 'C'
     */
    @Test
    public void checkCustomPayloadTwice() throws DotDataException, IOException {
        final String eventName = "Test_Event";
        final CustomAttributeCache analyticsCustomAttributeCache = CacheLocator.getAnalyticsCustomAttributeCache();
        assertNull(analyticsCustomAttributeCache.get(eventName));

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final long countBefore = getCount("A", "custom_1",
                "B", "custom_2");
        assertEquals(0, countBefore);

        final Map<String, Object> customAttributesPayload_1 = Map.of("A", "A_value_1",
                "B", "B_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload_1);

        final List<Map<String, Object>> dataBaseAttributes_1 = get(eventName);
        assertEquals(1, dataBaseAttributes_1.size());

        final Map<String, Object> customAttributes_1 =
                JsonUtil.getJsonFromString(dataBaseAttributes_1.get(0).get("custom_attribute").toString());

        assertEquals(2, customAttributes_1.size());
        assertTrue(customAttributes_1.containsKey("A"));
        assertTrue(customAttributes_1.containsKey("B"));

        assertTrue(customAttributes_1.containsValue("custom_1"));
        assertTrue(customAttributes_1.containsValue("custom_2"));

        final Map<String, Object> customAttributesPayload_2 = Map.of("A", "A_value_2",
                "C", "C_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload_2);

        final List<Map<String, Object>> dataBaseAttributes_2 = get(eventName);
        assertEquals(1, dataBaseAttributes_2.size());

        final Map<String, Object> customAttributes_2 =
                JsonUtil.getJsonFromString(dataBaseAttributes_2.get(0).get("custom_attribute").toString());

        assertEquals(3, customAttributes_2.size());
        assertTrue(customAttributes_2.containsKey("A"));
        assertTrue(customAttributes_2.containsKey("B"));
        assertTrue(customAttributes_2.containsKey("C"));

        assertTrue(customAttributes_2.containsValue("custom_1"));
        assertTrue(customAttributes_2.containsValue("custom_2"));
        assertTrue(customAttributes_2.containsValue("custom_3"));

        assertEquals("custom_3", customAttributes_2.get("C"));
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)}
     * When: called the method 6 times with a set of custom attributes match with 10 new attributes each time
     * Should: throw a {@link MaxCustomAttributesReachedException} in the last called
     */
    @Test
    public void checkCustomPayloadValidationMaxLimitReachedInSeveralCalled() throws DotDataException {
        for (int i = 0; i < 6; i++) {
            final Map<String, Object> customAttributesMatch = new HashMap<>();

            for (int k = 0; k < 10; k++) {
                int index = (i * 10) + k;
                customAttributesMatch.put("A" + index, "value_" + index);
            }

            try {
                APILocator.getAnalyticsCustomAttribute().checkCustomPayloadValidation("test", customAttributesMatch);
                assertTrue("MaxCustomAttributesReachedException Expected", i < 5);
            } catch (MaxCustomAttributesReachedException e) {
                assertEquals("Exception expected just in the last called", 5, i);
            }
        }
    }

    /**
     * Method to test {@link CustomAttributeAPIImpl#translateToDatabase(String, Map)} (String, Map)}
     * when: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} the follow payload:
     * <pre>
     * {
     *     "name": "name_value",
     *     "title": "title_value"
     * }
     * </pre>
     *
     * should: return
     *
     * <pre>
     * {
     *     "custom_1": [name or title],
     *     "custom_2": [if custom_1 is equals to "name" then custom_2 must be "title" otherwise it must be "name"]
     * }
     * </pre>
     *
     * when called the method {@link CustomAttributeAPIImpl#translateToDatabase(String, Map)} (String, Map)}
     *
     * @throws DotDataException
     */
    @Test
    public void translateToDatabase() throws DotDataException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();
        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= Map.of("name", "name_value",
                "title", "title_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);


        final Map<String, Object> payloadToDatabase =
                customAttributeAPI.translateToDatabase(eventName, customAttributesPayload);

        assertEquals(2, payloadToDatabase.size());
        assertTrue(payloadToDatabase.containsKey("custom_1"));
        assertTrue(payloadToDatabase.containsKey("custom_2"));

        if (payloadToDatabase.get("custom_1").equals("name_value")) {
            assertEquals("name_value", payloadToDatabase.get("custom_1"));
            assertEquals("title_value", payloadToDatabase.get("custom_2"));
        } else if (payloadToDatabase.get("custom_1").equals("title_value")) {
            assertEquals("name_value", payloadToDatabase.get("custom_2"));
            assertEquals("title_value", payloadToDatabase.get("custom_1"));
        }else {
            throw new DotDataException("Value not expected");
        }
    }


    /**
     * Method to test {@link CustomAttributeAPIImpl#translateToDatabase(String, Map)} (String, Map)}
     * when: Try to call the method with custom attribute that does not exists
     * should: throw an Exception
     *
     * @throws DotDataException
     */
    @Test
    public void translateToDatabaseWithNoExistsAttributes() throws DotDataException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();
        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= Map.of("name", "name_value",
                "title", "title_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final Map<String, Object> customAttributesPayload_2 = Map.of("another_attribute", "any_value");
        try {
            customAttributeAPI.translateToDatabase(eventName, customAttributesPayload_2);
            throw new RuntimeException("InvalidAttributeException exception expected");
        } catch (InvalidAttributeException e) {
            //expected
        }
    }


    /**
     * Method to test {@link CustomAttributeAPIImpl#translateToDatabase(String, Map)} (String, Map)}
     * when: Try to call the method when does not exists any match for the event
     * should: throw an Exception
     *
     * @throws DotDataException
     */
    @Test
    public void translateToDatabaseWithNoMatch() throws DotDataException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();
        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= Map.of("name", "name_value",
                "title", "title_value");

        try {
            customAttributeAPI.translateToDatabase(eventName, customAttributesPayload);
            throw new RuntimeException("MissingCustomAttributeMatchException exception expected");
        } catch (MissingCustomAttributeMatchException e) {
            //expected
        }
    }

    private static long getCount(final String customAttributeName1,
                                 final String customAttributeMatch1,
                                 final String customAttributeName2,
                                 final String customAttributeMatch2) throws DotDataException {
        return Long.parseLong(
                new DotConnect()
                        .setSQL("SELECT count(*) FROM analytic_custom_attributes " +
                                "WHERE custom_attribute->>'"+ customAttributeName1 +"' = '" + customAttributeMatch1 + "' " +
                                "AND custom_attribute->>'" + customAttributeName2 + "' = '" + customAttributeMatch2 + "'")
                        .loadObjectResults()
                        .get(0).get("count").toString());
    }

    private static List<Map<String, Object>> get(final String eventTypeName) throws DotDataException {
        return new DotConnect()
                .setSQL("SELECT * FROM analytic_custom_attributes WHERE event_type = '" + eventTypeName + "'")
                .loadObjectResults();
    }

    private static long getCount(final String customAttributeName1,
                                 final String customAttributeMatch1,
                                 final String customAttributeName2,
                                 final String customAttributeMatch2,
                                 final String customAttributeName3,
                                 final String customAttributeMatch3) throws DotDataException {
        return Long.parseLong(
                new DotConnect()
                        .setSQL("SELECT count(*) FROM analytic_custom_attributes " +
                                "WHERE custom_attribute->>'"+ customAttributeName1 +"' = '" + customAttributeMatch1 + "' " +
                                "AND custom_attribute->>'" + customAttributeName2 + "' = '" + customAttributeMatch2 + "'" +
                                "AND custom_attribute->>'" + customAttributeName3 + "' = '" + customAttributeMatch3 + "'")
                        .loadObjectResults()
                        .get(0).get("count").toString());
    }
}
