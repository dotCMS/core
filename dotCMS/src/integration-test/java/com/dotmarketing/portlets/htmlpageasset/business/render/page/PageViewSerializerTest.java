package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link PageViewSerializerTest}
 * @author jsanca
 */
public class PageViewSerializerTest {

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: createObjectMapKeyValue
     * Given Scenario: key value but not key field
     * ExpectedResult: return the key value as string
     */
    @Test
    public void createObjectMapUrlContent_convert_url_map_no_keyvalue() throws Exception {

        final PageViewSerializer pageViewSerializer = new PageViewSerializer();
        final Map<String, Object> pageViewMap       = new HashMap<>();
        final Contentlet urlContent                 = new Contentlet();
        final String     keyValue                   = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        urlContent.getMap().put("title", "Test");
        urlContent.getMap().put("keyValue", keyValue);
        final ContentType contentType               = mock(ContentType.class);
        urlContent.setContentType(contentType);
        when(contentType.fields(KeyValueField.class)).thenReturn(new ArrayList<>());
        pageViewSerializer.createObjectMapKeyValue(urlContent, contentType, pageViewMap);

        assertTrue(pageViewMap.isEmpty());
    }

    /**
     * Method to test: createObjectMapKeyValue
     * Given Scenario: key value given
     * ExpectedResult: return the key value as map
     */
    @Test
    public void createObjectMapUrlContent_convert_url_map_keyvalue() throws Exception {

        final PageViewSerializer pageViewSerializer = new PageViewSerializer();
        final Map<String, Object> pageViewMap       = new HashMap<>();
        final Contentlet urlContent                 = new Contentlet();
        final String     keyValue                   = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        urlContent.getMap().put("title", "Test");
        urlContent.getMap().put("keyValue", keyValue);
        final ContentType contentType               = mock(ContentType.class);
        final KeyValueField keyValueField               = mock(KeyValueField.class);
        urlContent.setContentType(contentType);
        when(contentType.fields(KeyValueField.class)).thenReturn(Arrays.asList(keyValueField));
        when(keyValueField.variable()).thenReturn("keyValue");
        pageViewSerializer.createObjectMapKeyValue(urlContent, contentType, pageViewMap);

        assertFalse(pageViewMap.isEmpty());
        assertEquals(1, pageViewMap.size());
        assertTrue(Map.class.isInstance(pageViewMap.get("keyValue")));
        assertEquals("value1", Map.class.cast(pageViewMap.get("keyValue")).get("key1"));
        assertEquals("value2", Map.class.cast(pageViewMap.get("keyValue")).get("key2"));
    }
}
