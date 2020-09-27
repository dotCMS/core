package com.dotcms.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class JsonObjectViewSerializer extends JsonSerializer<JsonObjectView> {

    @Override
    public void serialize(final JsonObjectView jsonObjectView, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeRawValue(jsonObjectView.getJsonObject().toString());
    }
}
