package com.dotcms.rest.api.v3.contenttype;


import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Form to {@link FieldResource#updateField(String, String, UpdateFieldForm, HttpServletRequest)}
 */
@JsonDeserialize(builder = UpdateFieldForm.Builder.class)
class UpdateFieldForm {

    private final Field field;

    public UpdateFieldForm(final Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    public static final class Builder {
        @JsonProperty
        private Map<String, Object> field;


        public Builder field(final Map<String, Object> field) {
            this.field = field;
            return this;
        }

        public UpdateFieldForm build(){
            final JsonFieldTransformer jsonFieldTransformer =
                    new JsonFieldTransformer(new JSONObject(field).toString());
            return new UpdateFieldForm(jsonFieldTransformer.from());
        }
    }
}
