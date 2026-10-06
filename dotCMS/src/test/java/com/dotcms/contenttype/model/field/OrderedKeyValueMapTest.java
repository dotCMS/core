package com.dotcms.contenttype.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link OrderedKeyValueMap}.
 *
 * <p>The primary invariant under test: even when the Jackson {@code ObjectMapper} is configured
 * with {@code ORDER_MAP_ENTRIES_BY_KEYS=true} (the same setting used by
 * {@code DotObjectMapperProvider} in the REST layer), serializing an {@link OrderedKeyValueMap}
 * must write keys in their <em>insertion order</em>, not alphabetical order.</p>
 */
public class OrderedKeyValueMapTest {

    /**
     * Builds an ObjectMapper that mirrors the sort settings from DotObjectMapperProvider so the
     * test exercises the exact condition that was causing alphabetical reordering.
     */
    private static ObjectMapper sortingMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper;
    }

    /**
     * Verifies that a sort-enabled ObjectMapper respects {@link OrderedKeyValueMap}'s custom
     * serializer and writes keys in insertion order (D, B, C, A), not alphabetical (A, B, C, D).
     */
    @Test
    public void testSerializerPreservesInsertionOrder() throws Exception {
        final OrderedKeyValueMap map = new OrderedKeyValueMap();
        map.put("D", "d");
        map.put("B", "b");
        map.put("C", "c");
        map.put("A", "a");

        final String json = sortingMapper().writeValueAsString(map);

        // Extract key order from the serialized JSON
        final List<String> keyOrder = extractKeyOrder(json);
        assertEquals("Keys must be written in insertion order (D,B,C,A), not alphabetical",
                Arrays.asList("D", "B", "C", "A"), keyOrder);
    }

    /**
     * Verifies that a plain LinkedHashMap with the same entries IS reordered alphabetically by the
     * sort-enabled ObjectMapper, confirming that OrderedKeyValueMap's custom serializer is what
     * preserves order (not some ObjectMapper quirk).
     */
    @Test
    public void testPlainLinkedHashMapIsReorderedByMapper() throws Exception {
        final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("D", "d");
        map.put("B", "b");
        map.put("C", "c");
        map.put("A", "a");

        final String json = sortingMapper().writeValueAsString(map);

        final List<String> keyOrder = extractKeyOrder(json);
        assertEquals("A plain LinkedHashMap must be reordered alphabetically by ORDER_MAP_ENTRIES_BY_KEYS",
                Arrays.asList("A", "B", "C", "D"), keyOrder);
    }

    /** Null values must survive a serialization round-trip without NPE. */
    @Test
    public void testSerializerHandlesNullValue() throws Exception {
        final OrderedKeyValueMap map = new OrderedKeyValueMap();
        map.put("key1", "value1");
        map.put("key2", null);
        map.put("key3", "value3");

        final String json = sortingMapper().writeValueAsString(map);

        assertNotNull(json);
        assertTrue("Serialized JSON must contain key2", json.contains("\"key2\""));

        @SuppressWarnings("unchecked")
        final Map<String, Object> parsed = sortingMapper().readValue(json, Map.class);
        assertTrue("key2 must be present in the parsed map", parsed.containsKey("key2"));
        assertNull("key2 must round-trip as null", parsed.get("key2"));
    }

    /** The copy constructor must preserve the iteration order of the source LinkedHashMap. */
    @Test
    public void testCopyConstructorPreservesOrder() throws Exception {
        final LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        source.put("Z", "z");
        source.put("M", "m");
        source.put("A", "a");

        final OrderedKeyValueMap copy = new OrderedKeyValueMap(source);

        final String json = sortingMapper().writeValueAsString(copy);
        final List<String> keyOrder = extractKeyOrder(json);
        assertEquals("Copy constructor must preserve source insertion order (Z,M,A)",
                Arrays.asList("Z", "M", "A"), keyOrder);
    }

    /** Extracts the ordered list of top-level keys from a flat JSON object string. */
    private static List<String> extractKeyOrder(final String json) {
        // Simple regex-free extraction: find all "KEY": patterns in order
        final java.util.List<String> keys = new java.util.ArrayList<>();
        int pos = 0;
        while (pos < json.length()) {
            final int quote = json.indexOf('"', pos);
            if (quote < 0) break;
            final int closeQuote = json.indexOf('"', quote + 1);
            if (closeQuote < 0) break;
            final String candidate = json.substring(quote + 1, closeQuote);
            // A key is followed by ':'
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
