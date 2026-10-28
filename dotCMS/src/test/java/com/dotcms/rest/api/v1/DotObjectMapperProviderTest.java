package com.dotcms.rest.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for the object mappers served by {@link DotObjectMapperProvider}, with a focus on the mapper
 * whose output is meant to be embedded inside an HTML {@code <script>} element as a JavaScript literal.
 */
class DotObjectMapperProviderTest {

    private static final String LINE_SEPARATOR = String.valueOf((char) 0x2028);
    private static final String PARAGRAPH_SEPARATOR = String.valueOf((char) 0x2029);

    private final ObjectMapper scriptMapper = DotObjectMapperProvider.createScriptEmbeddableMapper();
    private final ObjectMapper defaultMapper = DotObjectMapperProvider.createDefaultMapper();

    @Test
    void scriptEmbeddableMapper_encodesScriptElementDelimitersInValues() throws Exception {
        // Given a map whose string value contains a closing script tag
        final Map<String, Object> value = Map.of("v", "</script>");

        // When it is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(value);

        // Then '<', '/' and '>' are written as unicode sequences
        assertEquals("{\"v\":\"\\u003C\\u002Fscript\\u003E\"}", json);
    }

    @Test
    void scriptEmbeddableMapper_encodesAmpersand() throws Exception {
        // Given a map whose string value contains '&'
        final Map<String, Object> value = Map.of("v", "a&b");

        // When it is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(value);

        // Then '&' is written as a unicode sequence
        assertEquals("{\"v\":\"a\\u0026b\"}", json);
    }

    @Test
    void scriptEmbeddableMapper_encodesUnicodeLineSeparators() throws Exception {
        // Given a string value holding U+2028 and U+2029
        final Map<String, Object> value = Map.of("v", "x" + LINE_SEPARATOR + "y" + PARAGRAPH_SEPARATOR + "z");

        // When it is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(value);

        // Then both are written as unicode sequences
        assertEquals("{\"v\":\"x\\u2028y\\u2029z\"}", json);
    }

    @Test
    void scriptEmbeddableMapper_keepsStandardJsonSequencesAndNonAscii() throws Exception {
        // Given a value with a double quote, a backslash and a non-ASCII letter
        final Map<String, Object> value = Map.of("v", "say \"hi\" \\ ñ");

        // When it is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(value);

        // Then the quote and backslash keep the standard JSON sequences and the letter stays literal
        assertEquals("{\"v\":\"say \\\"hi\\\" \\\\ ñ\"}", json);
    }

    @Test
    void scriptEmbeddableMapper_encodesNestedValues() throws Exception {
        // Given a closing script tag inside a list inside a nested map
        final Map<String, Object> value = Map.of("outer", Map.of("list", List.of("</script>")));

        // When the outer map is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(value);

        // Then the nested value is encoded the same way as a top-level one
        assertEquals("{\"outer\":{\"list\":[\"\\u003C\\u002Fscript\\u003E\"]}}", json);
    }

    @Test
    void scriptEmbeddableMapper_encodesMapKeys() throws Exception {
        // Given a map whose key is a closing script tag
        final Map<String, Object> value = Map.of("</script>", 1);

        // When it is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(value);

        // Then the key is encoded like a value
        assertEquals("{\"\\u003C\\u002Fscript\\u003E\":1}", json);
    }

    @Test
    void scriptEmbeddableMapper_encodesJsonNodeTrees() throws Exception {
        // Given an ObjectNode holding a text node with a closing script tag
        final ObjectNode node = scriptMapper.createObjectNode();
        node.put("v", "</script>");

        // When the node is written with the script-embeddable mapper
        final String json = scriptMapper.writeValueAsString(node);

        // Then the text is encoded the same way as a plain value
        assertEquals("{\"v\":\"\\u003C\\u002Fscript\\u003E\"}", json);
    }

    @Test
    void scriptEmbeddableMapper_outputParsesBackToTheSameValues() throws Exception {
        // Given a map with every encoded character, quotes, a backslash and a non-ASCII letter in keys and values
        final Map<String, Object> value = new LinkedHashMap<>();
        value.put("<k>", "</script> & \"q\" \\ ñ");
        value.put("a/b", "x" + LINE_SEPARATOR + "y");
        value.put("plain", List.of("<i>", "ñ"));

        // When it is written with the script-embeddable mapper and read back with the default mapper
        final String json = scriptMapper.writeValueAsString(value);
        final Map<?, ?> parsed = defaultMapper.readValue(json, Map.class);

        // Then the parsed map equals the original
        assertEquals(value, parsed);
    }

    @Test
    void scriptEmbeddableMapper_matchesDefaultMapperWhenNothingNeedsEncoding() throws Exception {
        // Given a map with an Instant, an Optional and a list, and no character that needs encoding
        final Map<String, Object> value = new LinkedHashMap<>();
        value.put("b", Instant.ofEpochMilli(1700000000000L));
        value.put("a", Optional.of("x"));
        value.put("c", List.of(1, 2));

        // When it is written with both the default and the script-embeddable mapper
        final String fromDefault = defaultMapper.writeValueAsString(value);
        final String fromScript = scriptMapper.writeValueAsString(value);

        // Then both outputs are identical (same modules, key order and date format)
        assertEquals(fromDefault, fromScript);
        assertTrue(fromDefault.contains("1700000000000"));
    }

    @Test
    void defaultMapper_isNotAffectedByTheScriptEmbeddableMapper() throws Exception {
        // Given both mappers of the same provider
        final DotObjectMapperProvider provider = DotObjectMapperProvider.getInstance();
        final Map<String, Object> value = Map.of("v", "</script>");

        // When each one writes a value containing a closing script tag
        final String fromDefault = provider.getDefaultObjectMapper().writeValueAsString(value);
        final String fromScript = provider.getScriptEmbeddableObjectMapper().writeValueAsString(value);

        // Then only the script-embeddable one encodes it, and they do not share a factory
        assertEquals("{\"v\":\"</script>\"}", fromDefault);
        assertEquals("{\"v\":\"\\u003C\\u002Fscript\\u003E\"}", fromScript);
        assertNotSame(provider.getDefaultObjectMapper().getFactory(),
                provider.getScriptEmbeddableObjectMapper().getFactory());
    }

    @Test
    void scriptEmbeddableMapper_handlesEmptyValues() throws Exception {
        // Given an empty map, and a map with an empty string value
        // When they are written with the script-embeddable mapper
        // Then the outputs are {} and {"v":""}
        assertEquals("{}", scriptMapper.writeValueAsString(Map.of()));
        assertEquals("{\"v\":\"\"}", scriptMapper.writeValueAsString(Map.of("v", "")));
    }
}
