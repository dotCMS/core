package com.dotmarketing.portlets.containers.model;

import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ContainerViewSerializer extends JsonSerializer<ContainerView> {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private Map<Object, Object> asMap(final Object object)  {
        final ObjectWriter objectWriter = MAPPER.writer().withDefaultPrettyPrinter();

        try {
            final String json = objectWriter.writeValueAsString(object);
            final Map map = MAPPER.readValue(new CharArrayReader(json.toCharArray()), Map.class);

            map.values().removeIf(Objects::isNull);
            return map;
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void serialize(
            final ContainerView containerView,
            final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException {

        final ObjectWriter objectWriter = MAPPER.writer().withDefaultPrettyPrinter();
        final Map<Object, Object> map = this.asMap(containerView.getContainer());
        map.put("path", containerView.getPath());

        final String json = objectWriter.writeValueAsString(map);
        gen.writeRawValue(json);

    }
}
