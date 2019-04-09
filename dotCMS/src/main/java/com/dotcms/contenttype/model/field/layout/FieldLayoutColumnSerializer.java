package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;


import java.io.IOException;

public class FieldLayoutColumnSerializer extends JsonSerializer<FieldLayoutColumn> {
    @Override
    public void serialize(
            final FieldLayoutColumn fieldLayoutColumn,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer =
                new JsonFieldTransformer(fieldLayoutColumn.getColumn());
        jsonGenerator.writeObjectField("columnDivider", jsonFieldDividerTransformer.mapObject());

        final JsonFieldTransformer jsonColumnsTransformer =
                new JsonFieldTransformer(fieldLayoutColumn.getFields());
        jsonGenerator.writeObjectField("fields", jsonColumnsTransformer.mapList());

        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }
}
