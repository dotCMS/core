package com.dotcms.ai;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        return DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();
    }
}
