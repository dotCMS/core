package com.dotmarketing.portlets.contentlet.transform.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.OrderedKeyValueMap;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link KeyValueViewStrategy}.
 *
 * <p>The main invariant: after {@code transform()}, every KeyValue field value in the output map
 * must be an {@link OrderedKeyValueMap} so that Jackson's sort-enabled ObjectMapper (configured
 * in {@code DotObjectMapperProvider}) does not re-sort keys alphabetically in REST responses.</p>
 */
public class KeyValueViewStrategyTest {

    private static ObjectMapper sortingMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper;
    }

    private KeyValueViewStrategy strategy() {
        return new KeyValueViewStrategy(Mockito.mock(APIProvider.class));
    }

    /**
     * When the contentlet returns a plain {@link LinkedHashMap} for a KeyValue field (legacy
     * column path), {@code transform()} must wrap it in an {@link OrderedKeyValueMap}.
     */
    @Test
    public void testTransformWrapsPlainLinkedHashMapInOrderedKeyValueMap() throws Exception {
        final LinkedHashMap<String, Object> raw = new LinkedHashMap<>();
        raw.put("D", "d");
        raw.put("B", "b");

        final Map<String, Object> output = new HashMap<>();
        strategy().transform(mockContentletWithKeyValue("kvField", raw), output, Set.of(), null);

        assertTrue("Output value must be an OrderedKeyValueMap",
                output.get("kvField") instanceof OrderedKeyValueMap);
    }

    /**
     * When the contentlet already holds an {@link OrderedKeyValueMap} (JSON-column path where
     * {@link KeyValueField#asMap} is called), {@code transform()} must not double-wrap it —
     * the same instance must be reused.
     */
    @Test
    public void testTransformDoesNotDoubleWrapOrderedKeyValueMap() throws Exception {
        final OrderedKeyValueMap already = new OrderedKeyValueMap();
        already.put("D", "d");
        already.put("B", "b");

        final Map<String, Object> output = new HashMap<>();
        strategy().transform(mockContentletWithKeyValue("kvField", already), output, Set.of(), null);

        assertSame("An already-OrderedKeyValueMap must not be re-wrapped (same instance expected)",
                already, output.get("kvField"));
    }

    /**
     * End-to-end: after {@code transform()}, serializing the output with a sort-enabled
     * ObjectMapper must yield keys in insertion order (D, B, C, A), not alphabetical (A, B, C, D).
     */
    @Test
    public void testTransformPreservesInsertionOrderThroughSortingMapper() throws Exception {
        final LinkedHashMap<String, Object> raw = new LinkedHashMap<>();
        raw.put("D", "d");
        raw.put("B", "b");
        raw.put("C", "c");
        raw.put("A", "a");

        final Map<String, Object> output = new HashMap<>();
        strategy().transform(mockContentletWithKeyValue("kvField", raw), output, Set.of(), null);

        final String json = sortingMapper().writeValueAsString(output.get("kvField"));
        final List<String> keyOrder = extractKeyOrder(json);

        assertEquals("Insertion order D,B,C,A must survive serialization with ORDER_MAP_ENTRIES_BY_KEYS=true",
                Arrays.asList("D", "B", "C", "A"), keyOrder);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Creates a fully stubbed contentlet mock with one KeyValue field.
     * All mocks are created before any {@code Mockito.when()} calls to avoid
     * {@code UnfinishedStubbing} errors caused by nesting mock creation inside
     * {@code thenReturn()} arguments.
     */
    @SuppressWarnings("unchecked")
    private static Contentlet mockContentletWithKeyValue(final String variable,
            final Map<String, Object> value) {
        // Step 1: create all mocks
        final Field field = Mockito.mock(KeyValueField.class);
        final ContentType contentType = Mockito.mock(ContentType.class);
        final Contentlet contentlet = Mockito.mock(Contentlet.class);

        // Step 2: stub them (no mock creation inside when/thenReturn)
        Mockito.when(field.variable()).thenReturn(variable);
        Mockito.when(contentType.fields(KeyValueField.class)).thenReturn(List.of(field));
        Mockito.when(contentlet.getContentType()).thenReturn(contentType);
        Mockito.when(contentlet.getKeyValueProperty(variable)).thenReturn(value);
        return contentlet;
    }

    private static List<String> extractKeyOrder(final String json) {
        final java.util.List<String> keys = new java.util.ArrayList<>();
        int pos = 0;
        while (pos < json.length()) {
            final int quote = json.indexOf('"', pos);
            if (quote < 0) break;
            final int closeQuote = json.indexOf('"', quote + 1);
            if (closeQuote < 0) break;
            final String candidate = json.substring(quote + 1, closeQuote);
            int next = closeQuote + 1;
            while (next < json.length() && json.charAt(next) == ' ') next++;
            if (next < json.length() && json.charAt(next) == ':') {
                keys.add(candidate);
            }
            pos = closeQuote + 1;
        }
        return keys;
    }
}
