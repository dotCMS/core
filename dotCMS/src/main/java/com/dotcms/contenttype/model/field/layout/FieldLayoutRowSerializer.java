package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * {@link FieldLayoutRow} serializer, it serialize a json with the follow format:
 *
 * <pre>
 *     {
 *         divider: {
 *             ...
 *         }
 *         columns: [
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
 *     <li><b>divider:</b> it is the {@link com.dotcms.contenttype.model.field.FieldDivider} return by
 *     {@link FieldLayoutRow#getDivider()}</li>
 *     <li><b>columns:</b> COlumns into the row, return by {@link FieldLayoutRow#getColumns()}</li>
 * </ul>
 *
 * @see FieldLayoutColumnSerializer
 */
public class FieldLayoutRowSerializer extends JsonSerializer<FieldLayoutRow> {

    @Override
    public void serialize(
            final FieldLayoutRow fieldLayoutRow,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer = new JsonFieldTransformer(fieldLayoutRow.getDivider());
        jsonGenerator.writeObjectField("divider", jsonFieldDividerTransformer.mapObject());

        jsonGenerator.writeFieldName("columns");

        final ObjectMapper MAPPER = new ObjectMapper();
        MAPPER.writer()
                .withAttribute("type", serializerProvider.getAttribute("type"))
                .withAttribute("internationalization", serializerProvider.getAttribute("internationalization"))
                .writeValue(jsonGenerator, fieldLayoutRow.getColumns());

        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }
}