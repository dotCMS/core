package com.dotcms.graphql.datafetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class StoryBlockFieldDataFetcherTest extends UnitTestBase {

    private static final String FIELD_VAR = "subheading";

    private final StoryBlockFieldDataFetcher fetcher = new StoryBlockFieldDataFetcher();

    private DataFetchingEnvironment environmentWith(final Object fieldValue) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setIdentifier("test-identifier");
        if (fieldValue != null) {
            contentlet.getMap().put(FIELD_VAR, fieldValue);
        }

        final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getSource()).thenReturn(contentlet);
        when(environment.getField()).thenReturn(Field.newField(FIELD_VAR).build());
        return environment;
    }

    /**
     * Page queries: the transformer ({@code StoryBlockViewStrategy}) has already hydrated the
     * field into a Map. The fetcher must resolve it from the Map instead of round-tripping it
     * through {@code Map.toString()}.
     */
    @Test
    public void mapValue_isResolvedWithoutStringRoundTrip() throws Exception {
        final Map<String, Object> hydrated = new LinkedHashMap<>();
        hydrated.put("type", "doc");
        hydrated.put("content", List.of(Map.of(
                "type", "paragraph",
                "text", "Run your entire digital ecosystem - securely, visually, and without the chaos. https://dotcms.com")));

        final Map<String, Object> result = fetcher.get(environmentWith(hydrated));

        assertNotNull(result);
        assertEquals(hydrated, result.get("json"));
        assertNotSame("toMap must return a defensive copy of the hydrated value", hydrated, result.get("json"));
    }

    /**
     * Collection queries: the field still holds its stored JSON String. Text containing commas,
     * colons and URLs must parse correctly.
     */
    @Test
    public void jsonStringValue_withCommasAndUrls_isParsed() throws Exception {
        final String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"href\":\"https://dotcms.com/path?a=1&b=2\"},"
                + "\"text\":\"Securely, visually, and without the chaos; also: [brackets] {braces} \\\"quotes\\\" = # signs\"}]}";

        final Map<String, Object> result = fetcher.get(environmentWith(json));

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        final Map<String, Object> parsed = (Map<String, Object>) result.get("json");
        assertEquals("doc", parsed.get("type"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> paragraph = (Map<String, Object>) ((List<Object>) parsed.get("content")).get(0);
        assertEquals("Securely, visually, and without the chaos; also: [brackets] {braces} \"quotes\" = # signs",
                paragraph.get("text"));
        assertEquals("https://dotcms.com/path?a=1&b=2",
                ((Map<?, ?>) paragraph.get("attrs")).get("href"));
    }

    /**
     * A malformed value (e.g. the Java {@code Map.toString()} notation that used to reach the
     * legacy parser) must degrade gracefully: the field resolves to null, no exception thrown.
     */
    @Test
    public void malformedValue_resolvesToNull() throws Exception {
        final String javaMapNotation = "{type=doc, content=[{type=paragraph, text=Run your entire digital ecosystem - securely, visually}]}";

        assertNull(fetcher.get(environmentWith(javaMapNotation)));
    }

    @Test
    public void nullValue_resolvesToNull() throws Exception {
        assertNull(fetcher.get(environmentWith(null)));
    }

    @Test
    public void blankValue_resolvesToNull() throws Exception {
        assertNull(fetcher.get(environmentWith("  ")));
    }
}
