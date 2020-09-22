package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
        final Contentlet baseContent                = new Contentlet();
        final Contentlet urlContent                 = mock(Contentlet.class);
        final String     keyValue                   = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        baseContent.getMap().put("keyValue", keyValue);
        final ContentType contentType               = mock(ContentType.class);
        when(contentType.baseType()).thenReturn(BaseContentType.CONTENT);
        when(urlContent.getTitleImage()).thenReturn(Optional.empty());
        when(urlContent.getContentType()).thenReturn(contentType);
        when(urlContent.getBaseType()).thenReturn(Optional.of(BaseContentType.CONTENT));
        when(urlContent.get("keyValue")).thenReturn(keyValue);
        when(contentType.fields(KeyValueField.class)).thenReturn(new ArrayList<>());
        pageViewSerializer.createObjectMapUrlContent(urlContent, pageViewMap);

        assertTrue(pageViewMap.containsKey("urlContentMap"));
        assertFalse(Map.class.cast(pageViewMap.get("urlContentMap")).containsKey("keyValue"));
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
        final Contentlet baseContent                = new Contentlet();
        final Contentlet urlContent                 = mock(Contentlet.class);
        final String     keyValue                   = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        baseContent.getMap().put("keyValue", keyValue);
        final ContentType contentType               = mock(ContentType.class);
        final KeyValueField keyValueField           = mock(KeyValueField.class);
        when(urlContent.getTitleImage()).thenReturn(Optional.empty());
        when(contentType.baseType()).thenReturn(BaseContentType.CONTENT);
        when(urlContent.getContentType()).thenReturn(contentType);
        when(urlContent.getBaseType()).thenReturn(Optional.of(BaseContentType.CONTENT));
        when(urlContent.get("keyValue")).thenReturn(keyValue);
        when(contentType.fields(KeyValueField.class)).thenReturn(Arrays.asList(keyValueField));
        when(keyValueField.variable()).thenReturn("keyValue");
        when(urlContent.getKeyValueProperty("keyValue")).thenReturn(baseContent.getKeyValueProperty("keyValue"));
        pageViewSerializer.createObjectMapUrlContent(urlContent, pageViewMap);

        assertFalse(pageViewMap.isEmpty());
        assertEquals(1, pageViewMap.size());
        assertTrue(pageViewMap.containsKey("urlContentMap"));
        assertTrue((Map.class.cast(pageViewMap.get("urlContentMap")).containsKey("keyValue")));
        assertEquals("value1", Map.class.cast(Map.class.cast(pageViewMap.get("urlContentMap")).get("keyValue")).get("key1"));
        assertEquals("value2", Map.class.cast(Map.class.cast(pageViewMap.get("urlContentMap")).get("keyValue")).get("key2"));
    }
}
