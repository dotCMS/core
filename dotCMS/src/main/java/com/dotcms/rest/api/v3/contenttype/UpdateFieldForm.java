package com.dotcms.rest.api.v3.contenttype;


import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotmarketing.util.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Form to {@link FieldResource#updateFields(String, UpdateFieldForm, HttpServletRequest)}
 */
@JsonDeserialize(builder = UpdateFieldForm.Builder.class)
class UpdateFieldForm {

    private final List<Field> fields;

    public UpdateFieldForm(final List<Field> fields) {
        this.fields = fields;
    }

    public List<Field> getFields() {
        return fields;
    }

    public static final class Builder {
        @JsonProperty
        private List<Map<String, Object>> fields;


        public Builder fields(final List<Map<String, Object>> fields) {
            this.fields = fields;
            return this;
        }

        public UpdateFieldForm build(){
            final JsonFieldTransformer jsonFieldTransformer =
                    new JsonFieldTransformer(new JSONArray(fields).toString());
            return new UpdateFieldForm(jsonFieldTransformer.asList());
        }
    }
}
