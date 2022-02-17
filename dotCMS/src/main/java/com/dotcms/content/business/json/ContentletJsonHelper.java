package com.dotcms.content.business.json;

import com.dotcms.content.model.Contentlet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.Optional;

public class ContentletJsonHelper {

    /**
     * Short hand to parse the json but access directly the field of interest.
     * Beware: The Type specified on the Optional is the expected type returned in the field if the conversion fails the optional comes back empty
     * Be mindful of the expected type.
     * @param jsonInput
     * @param fieldName
     * @param <R>
     * @return
     */
    public <R> Optional<R> fieldValue(final String jsonInput, final String fieldName){
        final R fieldValue = Try.of(()-> (R)immutableFromJson(jsonInput).fields().get(fieldName).value()).getOrNull();
        return Optional.ofNullable(fieldValue);
    }

    /**
     *
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public Contentlet immutableFromJson(final String json) throws JsonProcessingException {
        return objectMapper.get().readValue(json, Contentlet.class);
    }

    /**
     *
     * @param object
     * @return
     * @throws JsonProcessingException
     */
    public String writeAsString(final Object object) throws JsonProcessingException {
        return objectMapper.get().writeValueAsString(object);
    }

    /**
     * Jackson mapper configuration and lazy initialized instance.
     */
    private final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    });

    public enum INSTANCE {
        INSTANCE;
        private final ContentletJsonHelper helper = new ContentletJsonHelper();

        public static ContentletJsonHelper get() {
            return INSTANCE.helper;
        }

    }
}
