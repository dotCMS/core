package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.field.layout.FieldLayoutRow;
import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.*;

import static com.dotcms.util.CollectionsUtils.map;

@JsonDeserialize(builder = MoveFieldsForm.Builder.class)
public class MoveFieldsForm {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Map<String, Object>> fields;

    public MoveFieldsForm(final List<Map<String, Object>> fields) {
        this.fields = fields;
    }

    public FieldLayout getRows(final String contentTypeId) {
        try {
            fixFields(fields, contentTypeId);
            final String rowsString = MAPPER.writeValueAsString(fields);
            final List<Field> fields=  MAPPER.readValue(rowsString,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Field.class)
            );

            return new FieldLayout(fields);
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void fixFields(final List<Map<String, Object>> fields, final String contentTypeId) {
        int layoutFieldIndex = 0;

        for (int i = 0; i < fields.size(); i++) {
            final Map<String, Object> fieldMap = fields.get(i);

            fieldMap.put("sortOrder", i);
            fieldMap.put("contentTypeId", contentTypeId);
            fieldMap.remove("dataType");

            final boolean isLayoutField = ImmutableRowField.class.getName().equals(fieldMap.get("clazz")) ||
                    ImmutableColumnField.class.getName().equals(fieldMap.get("clazz"));

            if (isLayoutField) {
                fieldMap.put("name", String.format("fields-$d", layoutFieldIndex++));
            }
        }
    }


    public static final class Builder {
        @JsonProperty
        private List<Map<String, Object>> layout;

        public MoveFieldsForm.Builder layout(final List<Map<String, Object>> layout) {
            this.layout = layout;
            return this;
        }

        public MoveFieldsForm build(){
            final List<Map<String, Object>> fieldsMap = new ArrayList<>();

            for (Map<String, Object> row : layout) {
                fieldsMap.add((Map) row.get("divider"));
                final List<Map<String, Object>> columnsMap = (List<Map<String, Object>>) row.get("columns");

                for (Map<String, Object> columnMap : columnsMap) {
                    fieldsMap.add((Map) columnMap.get("columnDivider"));
                    fieldsMap.addAll((List<Map<String, Object>>) columnMap.get("fields"));
                }
            }


            return new MoveFieldsForm(fieldsMap);
        }
    }
}
