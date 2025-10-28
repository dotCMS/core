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
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.time.Instant;

/**
 * Encapsulates the configuration for the Object Mapper on the Resources.
 *
 * @author jsanca
 */
public class DotObjectMapperProvider {


    private final ObjectMapper timestampObjectMapper;
    private final ObjectMapper iso8610ObjectMapper;

    private DotObjectMapperProvider() {
        this(createTimestampObjectMapper(), createISO8601DatesObjectMapper());
    }

    @VisibleForTesting
    protected DotObjectMapperProvider(
            final ObjectMapper timestampObjectMapper,
            final ObjectMapper iso8610ObjectMapper

    ) {
        this.timestampObjectMapper = timestampObjectMapper;
        this.iso8610ObjectMapper = iso8610ObjectMapper;
    }

    private static ObjectMapper buildBaseObjectMapper() {
        boolean alphaKeys = Config.getBooleanProperty("dotcms.rest.sort.json.properties", true);
        boolean useBlackbird = Config.getBooleanProperty("jackson.module.blackbird.enable", true);
        boolean useJdk8Module = Config.getBooleanProperty("jackson.module.jdk8module.enable", true);

        final ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new VersioningModule());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, alphaKeys);
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, alphaKeys);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.WRAP_EXCEPTIONS);
        if (useBlackbird) {
            objectMapper.registerModule(new BlackbirdModule());
        }
        if (useJdk8Module) {
            objectMapper.registerModule(new Jdk8Module());
        }

        return objectMapper;
    }

    private static ObjectMapper createTimestampObjectMapper() {

        ObjectMapper objectMapper = buildBaseObjectMapper();
        objectMapper.registerModule(createTimestampTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        objectMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private static ObjectMapper createISO8601DatesObjectMapper() {

        ObjectMapper objectMapper = buildBaseObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private static JavaTimeModule createTimestampTimeModule() {
        final JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Custom serializer for timestamp format (default mapper)
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

        // For ISO8601 mapper, use default JavaTimeModule behavior (ISO8601 strings)
        
        return javaTimeModule;
    }

    /**
     * Get the instance.
     *
     * @return DotObjectMapperProvider
     */
    public static DotObjectMapperProvider getInstance() {
        return DotObjectMapperProvider.SingletonHolder.INSTANCE;
    } // getInstance.


    /**
     * Gets the timestamp object mapper that writes Dates as timestamps.
     * For new code, prefer the {@link #getIso8610ObjectMapper()} instead.
     *
     * @return
     */
    public ObjectMapper getTimestampObjectMapper() {
        return timestampObjectMapper;
    }

    /**
     * Gets the default object mapper that writes Dates as timestamps.
     *
     * @return ObjectMapper
     *
     *
     * @deprecated Use {@link #getIso8610ObjectMapper()} instead, or if you need to write timestamps, use
     * {@link #getTimestampObjectMapper()}.
     */
    @Deprecated
    public ObjectMapper getDefaultObjectMapper() {
        return timestampObjectMapper;
    }


    /**
     * Gets the object mapper that writes Dates in the ISO8601 date format, e.g. 2022-09-27T18:00:00Z
     *
     * @return ObjectMapper
     */
    public ObjectMapper getIso8610ObjectMapper() {
        return iso8610ObjectMapper;
    }

    private static class SingletonHolder {

        private static final DotObjectMapperProvider INSTANCE = new DotObjectMapperProvider();
    }

} // E:O:F:DotObjectMapperProvider.
