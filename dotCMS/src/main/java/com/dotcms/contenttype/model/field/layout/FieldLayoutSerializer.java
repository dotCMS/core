package com.dotcms.contenttype.model.field.layout;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class FieldLayoutSerializer extends JsonSerializer<FieldLayout> {

    final ObjectMapper MAPPER;

    public FieldLayoutSerializer(){
        this(new ObjectMapper());
    }

    @VisibleForTesting
    FieldLayoutSerializer(final ObjectMapper mapper){
        super();
        this.MAPPER = mapper;
    }

    @Override
    public void serialize(
            final FieldLayout value,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializers)
            throws IOException {

        MAPPER.writer()
                .withAttribute("type", value.getContentType())
                .withAttribute("internationalization", value.getContentTypeInternationalization())
                .writeValue(jsonGenerator, value.getRows());
    }
}