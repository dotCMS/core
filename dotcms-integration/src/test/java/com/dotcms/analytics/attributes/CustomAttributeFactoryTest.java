package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CustomAttributeFactoryTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void cleanTable() throws Exception {
        new DotConnect().setSQL("DELETE FROM analytic_custom_attributes").loadResult();
    }

    /**
     * Method to test: {@link CustomAttributeFactoryImpl#save(String, Map)}
     * When: called the method with a {@link EventType} that does not exist
     * Should: save it
     */
    @Test
    public void saveNotExists() throws DotDataException {

        final CustomAttributeFactory factory = new CustomAttributeFactoryImpl();
        final Map<String, String> customPayload = Map.of("saveNotExistsAttribute", "attributeValue",
                "saveNotExistsAnotherAttribute", "anotherAttributeValue");

        long beforeCount = getCustomAttributesCount();

        factory.save(EventType.PAGE_VIEW.getName(), customPayload);
        long afterCount = getCustomAttributesCount();

        assertEquals(beforeCount + 1, afterCount);

        final long specificCount = Long.parseLong(
                new DotConnect()
                .setSQL("SELECT count(*) FROM analytic_custom_attributes " +
                        "WHERE custom_attribute->>'saveNotExistsAttribute' = 'attributeValue' " +
                        "AND custom_attribute->>'saveNotExistsAnotherAttribute' = 'anotherAttributeValue'")
                .loadObjectResults()
                .get(0).get("count").toString());

        assertEquals(1, specificCount);
    }

    private static long getCustomAttributesCount() throws DotDataException {
        return Long.parseLong(new DotConnect()
                .setSQL("SELECT count(*) FROM analytic_custom_attributes")
                .loadObjectResults()
                .get(0).get("count").toString());
    }


    /**
     * Method to test: {@link CustomAttributeFactoryImpl#save(String, Map)}
     * When: called the method with a {@link EventType} that exist
     * Should: update it
     */
    @Test
    public void saveExists() throws DotDataException {
        final CustomAttributeFactory factory = new CustomAttributeFactoryImpl();

        factory.save(EventType.PAGE_VIEW.getName(), Map.of("A", "a"));
        factory.save(EventType.PAGE_VIEW.getName(), Map.of("B", "b"));

        assertEquals(Map.of("B", "b"), factory.getAll().get(EventType.PAGE_VIEW.getName()));
    }

    /**
     * Method to test: {@link CustomAttributeFactoryImpl#getAll()}
     * When: Called this method
     * Should: return all the register in the custom attribute database
     *
     * @throws DotDataException
     */
    @Test
    public void getAllCustomAttributes() throws DotDataException {
        final CustomAttributeFactory factory = new CustomAttributeFactoryImpl();

        final Map<String, String> customAttributes = Map.of("A", "a", "B", "b");
        insertCustomAttributeMatch(EventType.PAGE_VIEW, customAttributes);

        Map<String, Map<String, String>> all = factory.getAll();

        assertEquals(1, all.size());
        assertEquals(customAttributes, all.get(EventType.PAGE_VIEW.getName()));
    }

    private static void insertCustomAttributeMatch(final EventType eventType,
                                                   final Map<String, String> customAttributes)
            throws DotDataException {
        new DotConnect().setSQL("INSERT INTO analytic_custom_attributes VALUES(?, ?)")
                .addParam(eventType.getName())
                .addJSONParam(customAttributes)
                .loadObjectResults();
    }
}
