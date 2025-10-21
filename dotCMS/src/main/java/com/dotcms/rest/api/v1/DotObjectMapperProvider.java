package com.dotcms.rest.api.v1;

import com.dotmarketing.util.Config;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.time.Instant;

/**
 * Encapsulates the configuration for the Object Mapper on the Resources.
 * @author jsanca
 */
public class DotObjectMapperProvider {


    private final ObjectMapper defaultObjectMapper;

    /**
     * Gets the default object mapper.
     * @return ObjectMapper
     */
    public ObjectMapper getDefaultObjectMapper() {
        return defaultObjectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return defaultObjectMapper;
    }

    private DotObjectMapperProvider() {
        this(createDefaultMapper());
    }

    @VisibleForTesting
    protected DotObjectMapperProvider(final ObjectMapper defaultObjectMapper) {
        this.defaultObjectMapper = defaultObjectMapper;
    }

    private static ObjectMapper createDefaultMapper() {

        boolean alphaKeys = Config.getBooleanProperty("dotcms.rest.sort.json.properties", true);
        boolean useBlackbird = Config.getBooleanProperty("jackson.module.blackbird.enable", true);
        boolean useJdk8Module = Config.getBooleanProperty("jackson.module.jdk8module.enable", false);


        final ObjectMapper result = new ObjectMapper();
        result.disable(DeserializationFeature.WRAP_EXCEPTIONS);

        result.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, alphaKeys);
        result.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, alphaKeys);
        result.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        if (useBlackbird) {
            result.registerModule(new BlackbirdModule());
        }
        if (useJdk8Module) {
            result.registerModule(new Jdk8Module());
        }

        result.registerModule(createJavaTimeModule());
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
