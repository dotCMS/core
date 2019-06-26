package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


import java.io.IOException;

/**
 * {@link FieldLayoutColumn} serializer, it serialize a json with the follow format:
 *
 * <pre>
 *     {
 *         columnDivider: {
 *             ...
 *         }
 *         fields: [
 *            {
 *                ...
 *            }
 *         ]
 *     }
 * </pre>
 *
 * where:
 *
 * <ul>
 *     <li><b>columnDivider:</b> it is the {@link com.dotcms.contenttype.model.field.ColumnField} return by
 *     {@link FieldLayoutColumn#getColumn()}</li>
 *     <li><b>fields:</b> Fields into the column, return by {@link FieldLayoutColumn#getFields()}</li>
 * </ul>
 */
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