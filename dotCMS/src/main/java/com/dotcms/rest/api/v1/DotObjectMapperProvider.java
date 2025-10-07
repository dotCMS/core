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
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import java.io.IOException;
import java.time.Instant;

/**
 * Encapsulates the configuration for the Object Mapper on the Resources.
 * @author jsanca
 */
public class DotObjectMapperProvider {

    final static Lazy<Boolean> ALPHA_KEYS = Lazy.of(()->Config.getBooleanProperty("dotcms.rest.sort.json.properties", true));
    final static Lazy<Boolean> USE_BLACKBIRD = Lazy.of(()->Config.getBooleanProperty("jackson.module.blackbird.enable", true));
    final static Lazy<Boolean> USE_JDK8_MODULE = Lazy.of(()->Config.getBooleanProperty("jackson.module.jdk8module.enable", false));
    private final ObjectMapper defaultObjectMapper;

    /**
     * Get's the default object mapper.
     * @return ObjectMapper
     */
    public ObjectMapper getDefaultObjectMapper() {
        return defaultObjectMapper;
    }

    private DotObjectMapperProvider() {
        this(createDefaultMapper());
    }

    @VisibleForTesting
    protected DotObjectMapperProvider(final ObjectMapper defaultObjectMapper) {
        this.defaultObjectMapper = defaultObjectMapper;
    }

    public static ObjectMapper createDefaultMapper() {

        final ObjectMapper result = new ObjectMapper();
        result.disable(DeserializationFeature.WRAP_EXCEPTIONS);

        result.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, ALPHA_KEYS.get());
        result.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, ALPHA_KEYS.get());
        if(USE_BLACKBIRD.get()) {
            result.registerModule(new BlackbirdModule());
        }
        if(USE_JDK8_MODULE.get()){
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
