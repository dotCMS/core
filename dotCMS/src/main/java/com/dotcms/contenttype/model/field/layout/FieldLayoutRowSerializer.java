package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class FieldLayoutRowSerializer extends JsonSerializer<FieldLayoutRow> {

    @Override
    public void serialize(
            final FieldLayoutRow fieldLayoutRow,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer =
                new JsonFieldTransformer((Field) fieldLayoutRow.getDivider());
        jsonGenerator.writeObjectField("divider", jsonFieldDividerTransformer.mapObject());
        jsonGenerator.writeObjectField("columns", fieldLayoutRow.getColumns());

        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }
}
