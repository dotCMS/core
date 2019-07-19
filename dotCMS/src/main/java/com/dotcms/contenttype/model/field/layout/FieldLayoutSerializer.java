package com.dotcms.contenttype.model.field.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class FieldLayoutSerializer extends JsonSerializer<FieldLayout> {

    @Override
    public void serialize(
            final FieldLayout value,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializers)
            throws IOException {

        final ObjectMapper MAPPER = new ObjectMapper();

        MAPPER.writer()
                .withAttribute("type", value.getContentType())
                .withAttribute("internationalization", value.getContentTypeInternationalization())
                .writeValue(jsonGenerator, value.getRows());
    }
}