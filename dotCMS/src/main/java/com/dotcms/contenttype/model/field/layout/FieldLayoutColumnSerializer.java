package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        final List<Map<String, Object>> fieldsMap = getFieldInternationalization(serializerProvider,
                jsonColumnsTransformer.mapList());

        jsonGenerator.writeObjectField("fields", fieldsMap);

        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }

    private List<Map<String, Object>> getFieldInternationalization(final SerializerProvider serializerProvider,
                                                                   final List<Map<String, Object>> fieldsMap) {

        final List<Map<String, Object>> fieldsInternationalizationMap = new ArrayList<>();
        final ContentTypeInternationalization contentTypeInternationalization =
                (ContentTypeInternationalization) serializerProvider.getAttribute("internationalization");

        if (contentTypeInternationalization != null) {
            final ContentType contentType = (ContentType) serializerProvider.getAttribute("type");

            for (final Map<String, Object> fieldMap : fieldsMap) {
                final Map<String, Object> fieldInternationalizationMap = APILocator.getContentTypeFieldAPI()
                        .getFieldInternationalization(contentType, contentTypeInternationalization, fieldMap);
                fieldsInternationalizationMap.add(fieldInternationalizationMap);
            }
        } else {
            fieldsInternationalizationMap.addAll(fieldsMap);
        }

        return fieldsInternationalizationMap;
    }
}