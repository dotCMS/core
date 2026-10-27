package com.dotcms.rest.api.v1;

import com.dotmarketing.util.Config;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Instant;

/**
 * Encapsulates the configuration for the Object Mapper on the Resources. 
 * @author jsanca
 */
public class DotObjectMapperProvider {

    private final ObjectMapper defaultObjectMapper;
    private final ObjectMapper scriptEmbeddableObjectMapper;

    /**
     * Get's the default object mapper.
     * @return ObjectMapper
     */
    public ObjectMapper getDefaultObjectMapper() {
        return defaultObjectMapper;
    }

    /**
     * Returns the mapper to use when JSON is written inline into an HTML {@code <script>} element,
     * for example to seed a JavaScript variable from a JSP. It is a copy of the default mapper, so
     * modules, key order and date format are the same, with one difference in the text it produces:
     * inside string values and keys, {@code <}, {@code >}, {@code &}, {@code /} and the Unicode line
     * separators U+2028 and U+2029 are written as {@code \\uXXXX} sequences, which keeps the text inside
     * the script element and lets the browser parse it as a JavaScript literal. The parsed values are
     * identical to the ones the default mapper would produce.
     *
     * @return the script-embeddable {@link ObjectMapper}
     */
    public ObjectMapper getScriptEmbeddableObjectMapper() {
        return scriptEmbeddableObjectMapper;
    }


    private DotObjectMapperProvider() {
        this(createDefaultMapper());
    }

    /**
     * Builds the provider around the given default mapper; the script-embeddable mapper is derived
     * from it, so a test that supplies its own default mapper gets a matching pair.
     *
     * @param defaultObjectMapper the mapper to serve as the default one
     */
    @VisibleForTesting
    protected DotObjectMapperProvider(final ObjectMapper defaultObjectMapper) {
        this.defaultObjectMapper = defaultObjectMapper;
        this.scriptEmbeddableObjectMapper = toScriptEmbeddableMapper(defaultObjectMapper);
    }

    /**
     * Creates a mapper whose JSON text can be written inline into an HTML {@code <script>} element:
     * the configuration of {@link #createDefaultMapper()} plus the character handling described in
     * {@link #getScriptEmbeddableObjectMapper()}. The default mapper is not changed.
     *
     * @return a new script-embeddable {@link ObjectMapper}
     */
    public static ObjectMapper createScriptEmbeddableMapper() {
        return toScriptEmbeddableMapper(createDefaultMapper());
    }

    /**
     * Copies the given mapper and installs the character table that makes its output safe to embed in
     * a script element. The copy has its own {@code JsonFactory}, so the source mapper keeps writing
     * plain JSON.
     *
     * @param source the mapper whose configuration is copied
     * @return the copy, writing script-embeddable JSON
     */
    private static ObjectMapper toScriptEmbeddableMapper(final ObjectMapper source) {
        final ObjectMapper result = source.copy();
        result.getFactory().setCharacterEscapes(new ScriptEmbeddableCharacters());
        return result;
    }

    /**
     * Character table for JSON embedded in an HTML script element. On top of the standard JSON
     * sequences for quotes, backslashes and control characters, the markup delimiters {@code <},
     * {@code >}, {@code &} and {@code /} and the Unicode line separators U+2028 and U+2029 are written
     * as {@code \\uXXXX}. Every other character, non-ASCII letters included, is written as is.
     */
    private static class ScriptEmbeddableCharacters extends CharacterEscapes {

        private static final long serialVersionUID = 1L;
        private static final int LINE_SEPARATOR = 0x2028;
        private static final int PARAGRAPH_SEPARATOR = 0x2029;

        private final int[] asciiCodes;

        ScriptEmbeddableCharacters() {
            final int[] codes = CharacterEscapes.standardAsciiEscapesForJSON();
            codes['<'] = CharacterEscapes.ESCAPE_STANDARD;
            codes['>'] = CharacterEscapes.ESCAPE_STANDARD;
            codes['&'] = CharacterEscapes.ESCAPE_STANDARD;
            codes['/'] = CharacterEscapes.ESCAPE_STANDARD;
            this.asciiCodes = codes;
        }

        /**
         * Returns the handling of each ASCII character: the standard JSON table, with the four markup
         * delimiters marked to be written as {@code \\uXXXX}.
         *
         * @return the per-character table Jackson consults for ASCII input
         */
        @Override
        public int[] getEscapeCodesForAscii() {
            return asciiCodes;
        }

        /**
         * Returns the sequence to write for a non-ASCII character, or {@code null} to write it as is.
         * Only the two Unicode line separators get a sequence, because a JavaScript parser treats them
         * as line breaks inside a string literal.
         *
         * @param ch the code point being written
         * @return the replacement text, or {@code null}
         */
        @Override
        public SerializableString getEscapeSequence(final int ch) {
            if (ch == LINE_SEPARATOR) {
                return new SerializedString("\\u2028");
            }
            if (ch == PARAGRAPH_SEPARATOR) {
                return new SerializedString("\\u2029");
            }
            return null;
        }
    }

    public static ObjectMapper createDefaultMapper() {

        final ObjectMapper result = new ObjectMapper();
        result.disable(DeserializationFeature.WRAP_EXCEPTIONS);

        if (Config.getBooleanProperty("dotcms.rest.sort.json.properties", true)) {
            result.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            result.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        }
        result.registerModule(new Jdk8Module());
        final JavaTimeModule javaTimeModule = createJavaTimeModule();

        result.registerModule(javaTimeModule);
        result.registerModule(new GuavaModule());

        return result;
    }

    private static JavaTimeModule createJavaTimeModule() {
        final JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(final Instant value, final JsonGenerator genarator,
                    final SerializerProvider serializers)
                    throws IOException {
                genarator.writeNumber(String.valueOf(value.toEpochMilli()));
            }
        });

        javaTimeModule.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            @Override
            public Instant deserialize(final JsonParser parser, final DeserializationContext ctxt)
                    throws IOException {
                try {
                    final long longValue = parser.getLongValue();
                    return Instant.ofEpochMilli(longValue);
                } catch (JsonParseException e) {
                    return Instant.parse(parser.getValueAsString());
                }
            }
        });
        return javaTimeModule;
    }

    private static class SingletonHolder {
        private static final DotObjectMapperProvider INSTANCE = new DotObjectMapperProvider();
    }
    /**
     * Get the instance.
     * @return DotObjectMapperProvider
     */
    public static DotObjectMapperProvider getInstance() {

        return DotObjectMapperProvider.SingletonHolder.INSTANCE;
    } // getInstance.

} // E:O:F:DotObjectMapperProvider.


