package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.layout.FieldLayoutRow;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotmarketing.util.json.JSONArray;

import java.util.List;
import java.util.Map;

public class MoveFieldsForm {
    private final List<FieldLayoutRow> rows;

    public MoveFieldsForm(final List<FieldLayoutRow> rows) {
        this.rows = rows;
    }

    public List<FieldLayoutRow> getRows() {
        return rows;
    }

    public static final class Builder {
        @JsonProperty
        private List<FieldLayoutRow> rows;


        public MoveFieldsForm.Builder layout(final List<FieldLayoutRow> rows) {
            this.rows = rows;
            return this;
        }

        public MoveFieldsForm build(){
            return new MoveFieldsForm(rows);
        }
    }
}
