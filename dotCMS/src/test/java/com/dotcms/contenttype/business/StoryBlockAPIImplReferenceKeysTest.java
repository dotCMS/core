package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link StoryBlockAPIImpl#withReferenceKeysOnly(Object)}, the reduction that makes a
 * Story Block value safe to keep where different users read it.
 */
public class StoryBlockAPIImplReferenceKeysTest {

    private static final String DOCUMENT = "{\"type\":\"doc\",\"content\":["
            + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]},"
            + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"abc\",\"languageId\":1,"
            + "\"title\":\"t\",\"secretNote\":\"TOP-SECRET\"}}},"
            + "{\"type\":\"dotImage\",\"attrs\":{\"data\":{\"identifier\":\"img\",\"languageId\":2,"
            + "\"asset\":\"/dA/img\"}}}]}";

    private final StoryBlockAPIImpl api = new StoryBlockAPIImpl();

    /**
     * Method to test: {@link StoryBlockAPIImpl#withReferenceKeysOnly(Object)}
     * Given Scenario: a JSON document with a content and an image reference holding field data
     * ExpectedResult: both references keep only their identifier and language, the text node is untouched
     */
    @Test
    @SuppressWarnings("unchecked")
    public void referencesKeepOnlyTheirKeys() throws Exception {
        final Object reduced = api.withReferenceKeysOnly(DOCUMENT);

        assertTrue("A JSON string comes back as a JSON string", reduced instanceof String);
        assertFalse(reduced.toString().contains("TOP-SECRET"));
        assertFalse(reduced.toString().contains("/dA/img"));

        final Map<String, Object> map = api.toMap(reduced);
        final List<Map<String, Object>> nodes = (List<Map<String, Object>>) map.get("content");
        assertEquals("hello", ((Map<String, Object>) ((List<Object>) nodes.get(0).get("content")).get(0)).get("text"));
        final Map<String, Object> contentData = data(nodes.get(1));
        assertEquals(2, contentData.size());
        assertEquals("abc", contentData.get("identifier"));
        assertEquals(1, contentData.get("languageId"));
        final Map<String, Object> imageData = data(nodes.get(2));
        assertEquals(2, imageData.size());
        assertEquals("img", imageData.get("identifier"));
    }

    /**
     * Method to test: {@link StoryBlockAPIImpl#withReferenceKeysOnly(Object)}
     * Given Scenario: the value is a Map instead of a JSON string
     * ExpectedResult: a reduced Map comes back and the given one is not modified
     */
    @Test
    @SuppressWarnings("unchecked")
    public void mapValueIsReducedWithoutModifyingTheOriginal() throws Exception {
        final LinkedHashMap<String, Object> original = api.toMap(DOCUMENT);

        final Object reduced = api.withReferenceKeysOnly(original);

        assertTrue(reduced instanceof Map);
        assertFalse(api.toJson((Map<String, Object>) reduced).contains("TOP-SECRET"));
        assertTrue("The given document keeps its data", api.toJson(original).contains("TOP-SECRET"));
    }

    /**
     * Method to test: {@link StoryBlockAPIImpl#withReferenceKeysOnly(Object)}
     * Given Scenario: a null value, and a value that is not a Story Block document
     * ExpectedResult: null stays null and the other value comes back as it is
     */
    @Test
    public void valuesThatAreNotDocumentsAreReturnedAsTheyAre() {
        assertNull(api.withReferenceKeysOnly(null));
        final String html = "<p>just html with $velocity</p>";
        assertSame(html, api.withReferenceKeysOnly(html));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(final Map<String, Object> node) {
        return (Map<String, Object>) ((Map<String, Object>) node.get("attrs")).get("data");
    }
}
