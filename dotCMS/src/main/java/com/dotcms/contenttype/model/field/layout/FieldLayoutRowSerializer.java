package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;

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
    final ObjectMapper mapper;

    public FieldLayoutRowSerializer(){
        this(new ObjectMapper());
    }

    @VisibleForTesting
    FieldLayoutRowSerializer(final ObjectMapper mapper){
        super();
        this.mapper = mapper;
    }

    @Override
    public void serialize(
            final FieldLayoutRow fieldLayoutRow,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer = new JsonFieldTransformer(fieldLayoutRow.getDivider());
        jsonGenerator.writeObjectField("divider", jsonFieldDividerTransformer.mapObject());

        List<FieldLayoutColumn> columns = fieldLayoutRow.getColumns();
        if(!columns.isEmpty()) {
            jsonGenerator.writeFieldName("columns");
            mapper.writer()
                    .withAttribute("type", serializerProvider.getAttribute("type"))
                    .withAttribute("internationalization", serializerProvider.getAttribute("internationalization"))
                    .writeValue(jsonGenerator, columns);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }
}