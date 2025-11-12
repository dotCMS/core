package com.dotcms.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Marshaller {

    private static final ObjectMapper MAPPER = createObjectMapper();

    public static <T> void marshal(OutputStream output, T value) throws IOException {
        MAPPER.writeValue(output, value);
    }

    public static <T> String marshal(T value) throws IOException {
        return MAPPER.writeValueAsString(value);
    }

    public static <T> T unmarshal(InputStream input, Class<T> type) throws IOException {
        return MAPPER.readValue(input, type);
    }

    public static <T> T unmarshal(String input, Class<T> type) throws IOException {
        if (input == null) {
            return null;
        }
        return MAPPER.readValue(input, type);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
                .registerModule(new BlackbirdModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
