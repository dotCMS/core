package com.dotcms.analytics.attributes;

import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.model.ResultSetItem;
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
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name", "request.custom.type"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "member": "request.eventType",
     *                 "operator": "equals",
     *                 "values": ["custom_event"]
     *             }
     *         ]
     *     }
     * </code>
     * Should: return the follow:
     * <code>
     *     {
     *         "dimensions": ["request.custom_1", "request.custom_2"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "member": "request.eventType",
     *                 "operator": "equals",
     *                 "values": ["custom_event"]
     *             }
     *         ]
     *     }
     * </code>
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNames() throws DotDataException, CustomAttributeProcessingException {

        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
            "\"dimensions\": [\"request.custom.name\", \"request.custom.type\"],\n" +
            "\"measures\": [\"request.count\"],\n" +
            "\"filters\": [\n" +
                "{\n" +
                "   \"member\": \"request.eventType\",\n" +
                "   \"operator\": \"equals\",\n" +
                "   \"values\": [\"" + eventName + "\"]\n" +
                "}\n" +
            "]\n" +
        "}";

        final CustomAttributeAPI.TranslatedQuery translatedQuery = customAttributeAPI.translateFromFriendlyName(query);

        final String queryTranslated = translatedQuery.getTranslateQuery();

        final String expectedQuery = "{\n" +
            "\"dimensions\": [\"request.custom_1\", \"request.custom_2\"],\n" +
            "\"measures\": [\"request.count\"],\n" +
            "\"filters\": [\n" +
                "{\n" +
                "   \"member\": \"request.eventType\",\n" +
                "   \"operator\": \"equals\",\n" +
                "   \"values\": [\"" + eventName + "\"]\n" +
                "}\n" +
            "]\n" +
        "}";

        assertEquals(expectedQuery, queryTranslated);

        final Map<String, String> matchApplied = translatedQuery.getMatchApplied();
        assertEquals(3, matchApplied.size());

        assertTrue(matchApplied.containsKey("request.custom.name"));
        assertTrue(matchApplied.containsKey("request.custom.type"));
        assertTrue(matchApplied.containsKey("request.custom.anotherOne"));

        assertEquals("request.custom_1", matchApplied.get("request.custom.name"));
        assertEquals("request.custom_2", matchApplied.get("request.custom.type"));
        assertEquals("request.custom_3", matchApplied.get("request.custom.anotherOne"));
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * is called with a query like (no filter here):
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name", "request.custom.type"],
     *         "measures": ["request.count"]
     *     }
     * </code>
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithOutFilter() throws DotDataException {

        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\", \"request.custom.type\"],\n" +
                "\"measures\": [\"request.count\"]\n" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * is called with a query like (no filter here):
     *
     * <code>
     *     {
     *         "dimensions": ["request.name", "request.type"],
     *         "measures": ["request.count"]
     *     }
     * </code>
     *
     * Should: In this case the user is not using any custom attribute so we should just return the same query
     * without translate
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithOutFiltersAndCustom()
            throws DotDataException, CustomAttributeProcessingException {

        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.name\", \"request.type\"],\n" +
                "\"measures\": [\"request.count\"]\n" +
        "}";

        final CustomAttributeAPI.TranslatedQuery translatedQuery = customAttributeAPI.translateFromFriendlyName(query);

        assertEquals(query, translatedQuery.getTranslateQuery());

        final Map<String, String> matchApplied = translatedQuery.getMatchApplied();
        assertTrue(matchApplied.isEmpty());
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like this one:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name", "request.custom.type"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "member": "request.eventType",
     *                 "operator": "set"
     *             }
     *         ]
     *     }
     * </code>
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithoutFiltersValues() throws DotDataException, CustomAttributeProcessingException {

        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\", \"request.custom.type\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [\n" +
                    "{\n" +
                    "   \"member\": \"request.eventType\",\n" +
                    "   \"operator\": \"set\"\n" +
                    "}\n" +
                "]\n" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like this one:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name", "request.custom.type"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "member_miss-spelling": "request.eventType",
     *                 "operator": "set",
     *                 "values": ["eventType"]
     *             }
     *         ]
     *     }
     * </code>
     *
     * Member is misspelling
     *
     * Should: return the same query the method goal is translated the custom attribute the query not validate it.
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithoutFiltersMember() throws DotDataException, CustomAttributeProcessingException {

        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\", \"request.custom.type\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [\n" +
                    "{\n" +
                    "   \"member_miss-spelling\": \"request.eventType\",\n" +
                    "   \"operator\": \"set\",\n" +
                    "   \"values\": [\"" + eventName + "\"]\n" +
                    "}\n" +
                "]\n" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "or": [
     *                    {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                    },
     *                    {
     *                      "member": "request.custom.type",
     *                      "operator": "equals",
     *                      "values": ["A"]
     *                    },
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     * Should: return the follow:
     * <code>
     *     {
     *         "dimensions": ["request.custom_1"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "or": [
     *                    {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                    },
     *                    {
     *                      "member": "request.custom_2",
     *                      "operator": "equals",
     *                      "values": ["A"]
     *                    },
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithOrFilter() throws DotDataException, CustomAttributeProcessingException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"or\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName + "\"]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom.type\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                "]" +
        "}";

        final String queryTranslated = customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery();

        final String expectedQuery = "{\n" +
                "\"dimensions\": [\"request.custom_1\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"or\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName + "\"]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom_2\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                "]" +
        "}";

        assertEquals(expectedQuery, queryTranslated);
    }


    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "and": [
     *                    {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                    },
     *                    {
     *                      "member": "request.custom.type",
     *                      "operator": "equals",
     *                      "values": ["A"]
     *                    }
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     *
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithAndFilter() throws DotDataException, CustomAttributeProcessingException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"and\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName + "\"]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom.type\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                "]" +
        "}";

        final String queryTranslated = customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery();

        final String expectedQuery = "{\n" +
                "\"dimensions\": [\"request.custom_1\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"and\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName + "\"]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom_2\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                    "]" +
        "}";

        assertEquals(expectedQuery, queryTranslated);
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "or": {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                 }
     *             }
     *         ]
     *     }
     * </code>
     * (The or is invalid is must be an array)
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithInvalidOrFilter() throws DotDataException, CustomAttributeProcessingException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"or\": {\n" +
                            "\"member\": \"request.eventType\",\n" +
                            "\"operator\": \"equals\",\n" +
                            "\"values\": [\"" + eventName + "\"]\n" +
                        "}\n" +
                    "}\n" +
                "]" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "or": {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                  }
     *             }
     *         ]
     *     }
     * </code>
     * (The or is invalid is must be an array)
     *
     * Should:return the same query
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithInvalidOrFilterButNotCustomAttributes()
            throws DotDataException, CustomAttributeProcessingException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"or\": {\n" +
                            "\"member\": \"request.eventType\",\n" +
                            "\"operator\": \"equals\",\n" +
                            "\"values\": [\"" + eventName + "\"]\n" +
                        "}\n" +
                    "}\n" +
                "]" +
        "}";

        assertEquals(query, customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery());
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "and": {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                 }
     *             }
     *         ]
     *     }
     * </code>
     * (The or is invalid is must be an array)
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithInvalidAndFilter()
            throws DotDataException, CustomAttributeProcessingException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"and\": {\n" +
                            "\"member\": \"request.eventType\",\n" +
                            "\"operator\": \"equals\",\n" +
                            "\"values\": [\"" + eventName + "\"]\n" +
                        "}\n" +
                    "}\n" +
                "]" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "and": {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event"]
     *                  }
     *             }
     *         ]
     *     }
     * </code>
     * (The or is invalid is must be an array)
     *
     * Should:return the same query
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithInvalidAndFilterButNotCustomAttributes()
            throws DotDataException, CustomAttributeProcessingException {
        final String eventName = "Test_Event_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload= new LinkedHashMap<>();
        customAttributesPayload.put("name", "name_value");
        customAttributesPayload.put("type", "type_value");
        customAttributesPayload.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload);

        final String query = "{\n" +
                "\"dimensions\": [\"request.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"and\": {\n" +
                            "\"member\": \"request.eventType\",\n" +
                            "\"operator\": \"equals\",\n" +
                            "\"values\": [\"" + eventName + "\"]\n" +
                        "}\n" +
                    "}\n" +
                "]" +
        "}";

        assertEquals(query, customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery());
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event_1 as Event Type:
     *
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     *  and another call for custom_event_2:
     *
     * <code>
     * {
     *   name_2: "name_value",
     *   type_2: "type_value",
     *   anotherOne_2: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "and": [
     *                    {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event_1", "custom_events_2"]
     *                    },
     *                    {
     *                      "member": "request.custom.type",
     *                      "operator": "equals",
     *                      "values": ["A"]
     *                    }
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithMultiEventTypeValues() throws DotDataException {
        final String eventName_1 = "Test_Event_1_" + System.currentTimeMillis();
        final String eventName_2 = "Test_Event_2_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload_1 = new LinkedHashMap<>();
        customAttributesPayload_1.put("name", "name_value");
        customAttributesPayload_1.put("type", "type_value");
        customAttributesPayload_1.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName_1, customAttributesPayload_1);

        final Map<String, Object> customAttributesPayload_2 = new LinkedHashMap<>();
        customAttributesPayload_2.put("name", "name_value");
        customAttributesPayload_2.put("type", "type_value");
        customAttributesPayload_2.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName_2, customAttributesPayload_2);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"and\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName_1 + "\"," + "\"" + eventName_2 + "\"" + "]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom.type\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                "]" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }




    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event_1 as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     *  and another call for custom_event_2:
     *
     * <code>
     * {
     *   name_2: "name_value",
     *   type_2: "type_value",
     *   anotherOne_2: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "or": [
     *                    {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event_1", "custom_event_2"]
     *                    },
     *                    {
     *                      "member": "request.custom.type",
     *                      "operator": "equals",
     *                      "values": ["A"]
     *                    },
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithOrAndMultiEventTypeValues() throws DotDataException {
        final String eventName_1 = "Test_Event_1_" + System.currentTimeMillis();
        final String eventName_2 = "Test_Event_2_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload_1= new LinkedHashMap<>();
        customAttributesPayload_1.put("name", "name_value");
        customAttributesPayload_1.put("type", "type_value");
        customAttributesPayload_1.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName_1, customAttributesPayload_1);

        final Map<String, Object> customAttributesPayload_2= new LinkedHashMap<>();
        customAttributesPayload_2.put("name", "name_value");
        customAttributesPayload_2.put("type", "type_value");
        customAttributesPayload_2.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName_2, customAttributesPayload_2);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"or\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName_1 + "\"," + "\"" + eventName_2 + "\"]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom.type\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                "]" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event_1 as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     *  and another call for custom_event_2:
     *
     * <code>
     * {
     *   name_2: "name_value",
     *   type_2: "type_value",
     *   anotherOne_2: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.custom.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                 "and": [
     *                    {
     *                      "member": "request.eventType",
     *                      "operator": "equals",
     *                      "values": ["custom_event_1", "custom_event_2"]
     *                    },
     *                    {
     *                      "member": "request.custom.type",
     *                      "operator": "equals",
     *                      "values": ["A"]
     *                    },
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     *
     * Should: return a CustomAttributeProcessingException with a message as follows:
     * "It is impossible to determine the EventType to resolve the custom attribute match"
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWithAndOperatorAndMultiEventTypeValues() throws DotDataException {
        final String eventName_1 = "Test_Event_1_" + System.currentTimeMillis();
        final String eventName_2 = "Test_Event_2_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload_1= new LinkedHashMap<>();
        customAttributesPayload_1.put("name", "name_value");
        customAttributesPayload_1.put("type", "type_value");
        customAttributesPayload_1.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName_1, customAttributesPayload_1);

        final Map<String, Object> customAttributesPayload_2= new LinkedHashMap<>();
        customAttributesPayload_2.put("name", "name_value");
        customAttributesPayload_2.put("type", "type_value");
        customAttributesPayload_2.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName_2, customAttributesPayload_2);

        final String query = "{\n" +
                "\"dimensions\": [\"request.custom.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [ \n" +
                    "{\n" +
                        "\"and\": [\n" +
                            "{\n" +
                                "\"member\": \"request.eventType\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"" + eventName_1 + "\"," + "\"" + eventName_2 + "\"]\n" +
                            "},\n" +
                            "{\n" +
                                "\"member\": \"request.custom.type\",\n" +
                                "\"operator\": \"equals\",\n" +
                                "\"values\": [\"A\"]\n" +
                            "}\n" +
                        "]\n" +
                    "}\n" +
                "]" +
        "}";

        try {
            customAttributeAPI.translateFromFriendlyName(query);
            throw new AssertionError("CustomAttributeProcessingException expected");
        } catch (CustomAttributeProcessingException e) {
            assertEquals( "It is impossible to determine the EventType to resolve the custom attribute match",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * When: Send an invalid json
     * Should: return exactly the same "query", remember the method translate custom no validate the Json
     *
     * @throws CustomAttributeProcessingException
     */
    @Test
    public void translateToFriendlyNamesWithInvalidJson() throws CustomAttributeProcessingException {
        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();
        final String query = "Invalid json";

        assertEquals(query, customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery());
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * When: Send a invalid json with custom match
     * Should: return exactly the same "query", remember the method translate custom no validate the Json
     *
     * @throws CustomAttributeProcessingException
     */
    @Test
    public void translateToFriendlyNamesWithInvalidJsonWithCustom() throws CustomAttributeProcessingException {
        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();
        final String query = "Invalid json with request.custom.name";

        assertEquals(query, customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery());
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)}
     * WHen: called the {@link CustomAttributeAPIImpl#checkCustomPayloadValidation(String, Map)} with a custom payload
     * and custom_event as Event Type:
     * <code>
     * {
     *   name: "name_value",
     *   type: "type_value",
     *   anotherOne: "another_value"
     * }
     * </code>
     *
     * and then the {@link CustomAttributeAPIImpl#translateFromFriendlyName(String)} is called with a query like:
     *
     * <code>
     *     {
     *         "dimensions": ["request.name"],
     *         "measures": ["request.count"],
     *         "filters": [
     *             {
     *                "member": "request.eventType",
     *                "operator": "equals",
     *                 "values": ["custom_event"]
     *             }
     *         ]
     *     }
     * </code>
     * (The or is invalid is must be an array)
     *
     * Should:return the same query
     *
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void translateToFriendlyNamesWhenDoesNotExistsMatch() throws CustomAttributeProcessingException, DotDataException {
        final String eventName = "Test_Event_1_" + System.currentTimeMillis();

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();

        final Map<String, Object> customAttributesPayload_1= new LinkedHashMap<>();
        customAttributesPayload_1.put("name", "name_value");
        customAttributesPayload_1.put("type", "type_value");
        customAttributesPayload_1.put("anotherOne", "another_value");

        customAttributeAPI.checkCustomPayloadValidation(eventName, customAttributesPayload_1);

        final String query = "{\n" +
                "\"dimensions\": [\"request.name\"],\n" +
                "\"measures\": [\"request.count\"],\n" +
                "\"filters\": [\n" +
                    "{\n" +
                    "   \"member\": \"request.eventType\",\n" +
                    "   \"operator\": \"equals\",\n" +
                    "   \"values\": [\"" + eventName + "\"]\n" +
                    "}\n" +
                "]\n" +
        "}";

        final String queryTranslated = customAttributeAPI.translateFromFriendlyName(query).getTranslateQuery();

        assertEquals(query, queryTranslated);
    }

    /**
     * Method to test: {@link CustomAttributeAPIImpl#translateResults(ReportResponse, Map)}
     * When: You get a result after run the query like:
     * <code>
     *    [
     *      {
     *          "request.custom_1": "A",
     *          "request.custom_2": "B",
     *          "request.totalSessions": "10"
     *      },
     *      {
     *          "request.custom_1": "C",
     *          "request.custom_2": "D",
     *          "request.totalSessions": "20"
     *      }
     *    ]
     * </code>
     * Should: translate it to:
     * <code>
     *    [
     *      {
     *          "request.custom.name": "A",
     *          "request.custom.type": "B",
     *          "request.totalSessions": "10"
     *      },
     *      {
     *          "request.custom.name": "C",
     *          "request.custom.type": "D",
     *          "request.totalSessions": "20"
     *      }
     *    ]
     * </code>
     */
    @Test
    public void translateResult(){
        final ReportResponse originalReportResponse = mock(ReportResponse.class);

        final List<ResultSetItem> resultSetItems = new ArrayList<>();

        final ResultSetItem resultSetItem_1 = mock(ResultSetItem.class);
        when(resultSetItem_1.getAll()).thenReturn(Map.of(
                "request.custom_1", "A",
                "request.custom_2", "B",
                "request.totalSessions", "10"
        ));
        resultSetItems.add(resultSetItem_1);

        final ResultSetItem resultSetItem_2 = mock(ResultSetItem.class);
        when(resultSetItem_2.getAll()).thenReturn(Map.of(
                "request.custom_1", "C",
                "request.custom_2", "D",
                "request.totalSessions", "20"
        ));
        resultSetItems.add(resultSetItem_2);

        when(originalReportResponse.getResults()).thenReturn(resultSetItems);

        final Map<String, String> matchApplied = Map.of(
                "request.custom_1", "request.custom.name",
                "request.custom_2",  "request.custom.type"
        );

        final CustomAttributeAPI customAttributeAPI = APILocator.getAnalyticsCustomAttribute();
        final ReportResponse transslateReportResponse = customAttributeAPI.translateResults(originalReportResponse, matchApplied);

        final List<ResultSetItem> results = transslateReportResponse.getResults();

        assertEquals(2, results.size());

        final Map<String, String> expected_1 = Map.of(
                "request.custom.name", "A",
                "request.custom.type", "B",
                "request.totalSessions", "10"
        );

        final Map<String, String> expected_2 = Map.of(
                "request.custom.name", "C",
                "request.custom.type", "D",
                "request.totalSessions", "20"
        );

        assertEquals(expected_1, results.get(0).getAll());
        assertEquals(expected_2, results.get(1).getAll());

    }
}
