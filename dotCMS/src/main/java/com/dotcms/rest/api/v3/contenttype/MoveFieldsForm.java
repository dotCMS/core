package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.exception.ExceptionUtil.getErrorMessage;

/**
 * This form class is used by the
 * {@link FieldResource#moveFields(String, MoveFieldsForm, HttpServletRequest)} REST Endpoint to
 * persist changes in the field layout of a Content Type. This involves adding, moving, or deleting
 * a field from it.
 *
 * @author Freddy Rodriguez
 * @since Jun 24th, 2019
 */
@JsonDeserialize(builder = MoveFieldsForm.Builder.class)
public class MoveFieldsForm {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final List<Map<String, Object>> fields;

    public MoveFieldsForm(final List<Map<String, Object>> fields) {
        this.fields = fields;
    }

    /**
     * Generates the new Field Layout object with the new changes made to the Content Type's
     * fields.
     *
     * @param contentType The {@link ContentType} whose fields have been updated.
     *
     * @return The new {@link FieldLayout} object with the updated fields.
     */
    public FieldLayout getRows(final ContentType contentType) {
        try {
            fixFields(fields, contentType.id());
            final String rowsString = MAPPER.writeValueAsString(fields);
            return new FieldLayout(contentType, new JsonFieldTransformer(rowsString).asList());
        } catch (final IOException e) {
            Logger.error(MoveFieldsForm.class,
                    String.format("Error generating field layout in Content Type %s: %s", contentType.name(),
                            getErrorMessage(e)));
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Based on their order of appearance in the list of maps, this method sets the appropriate
     * order value for each field in the Content Type's layout
     *
     * @param fields        The list of maps containing each field in the Content Type.
     * @param contentTypeId The ID of the Content Type whose list of fields is being updated.
     */
    private void fixFields(final List<Map<String, Object>> fields, final String contentTypeId) {
        int layoutFieldIndex = 0;

        for (int i = 0; i < fields.size(); i++) {
            final Map<String, Object> fieldMap = fields.get(i);

            fieldMap.put("sortOrder", i);
            fieldMap.put("contentTypeId", contentTypeId);

            final boolean isLayoutField = ImmutableRowField.class.getName().equals(fieldMap.get("clazz")) ||
                    ImmutableColumnField.class.getName().equals(fieldMap.get("clazz"));

            if (isLayoutField) {
                fieldMap.put("name", String.format("fields-%d", layoutFieldIndex++));
            }
        }
    }

    @SuppressWarnings("unchecked")
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
                fieldsMap.add((Map<String, Object>) row.get("divider"));
                final List<Map<String, Object>> columnsMap = (List<Map<String, Object>>) row.get("columns");

                if (columnsMap != null) {
                    for (final Map<String, Object> columnMap : columnsMap) {
                        fieldsMap.add((Map<String, Object>) columnMap.get("columnDivider"));
                        fieldsMap.addAll((List<Map<String, Object>>) columnMap.get("fields"));
                    }
                }
            }
            return new MoveFieldsForm(fieldsMap);
        }

    }

}
