package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;

public class FieldColumnLayout {
    private Field[] fields;

    public FieldColumnLayout(Field[] fields) {
        this.fields = fields;
    }

    public Field[] getFields() {
        return fields;
    }
}
