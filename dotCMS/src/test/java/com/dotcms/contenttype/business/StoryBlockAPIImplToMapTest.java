package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link StoryBlockAPIImpl#toMap(Object)}, the single canonical conversion for
 * Block Editor values. It must accept both value shapes: a raw JSON String and an
 * already-hydrated Map (e.g. from {@code StoryBlockViewStrategy}).
 */
public class StoryBlockAPIImplToMapTest extends UnitTestBase {

    private final StoryBlockAPIImpl storyBlockAPI = new StoryBlockAPIImpl();

    @Test
    public void toMap_parsesJsonString() throws JsonProcessingException {
        final String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"text\":\"Securely, visually, and without the chaos: https://dotcms.com\"}]}";

        final LinkedHashMap<String, Object> result = storyBlockAPI.toMap(json);

        assertEquals("doc", result.get("type"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> paragraph = (Map<String, Object>) ((List<Object>) result.get("content")).get(0);
        assertEquals("Securely, visually, and without the chaos: https://dotcms.com", paragraph.get("text"));
    }

    /**
     * A hydrated Map must be copied, never round-tripped through {@code Map.toString()} — Java map
     * notation is not JSON and text containing commas/URLs breaks any parser fed with it.
     */
    @Test
    public void toMap_acceptsHydratedMap() throws JsonProcessingException {
        final Map<String, Object> hydrated = new LinkedHashMap<>();
        hydrated.put("type", "doc");
        hydrated.put("content", List.of(Map.of(
                "type", "paragraph",
                "text", "Run your entire digital ecosystem - securely, visually, and without the chaos.")));

        final LinkedHashMap<String, Object> result = storyBlockAPI.toMap(hydrated);

        assertEquals(hydrated, result);
        assertNotSame("toMap must return a defensive copy", hydrated, result);
    }

    @Test(expected = JsonProcessingException.class)
    public void toMap_rejectsJavaMapNotationString() throws JsonProcessingException {
        storyBlockAPI.toMap("{type=doc, content=[{type=paragraph, text=securely, visually}]}");
    }
}
