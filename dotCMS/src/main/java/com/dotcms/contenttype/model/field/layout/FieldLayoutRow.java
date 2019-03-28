package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldDivider;

public class FieldLayoutRow {
    private final FieldDivider fieldDivider;
    private final FieldColumnLayout[] columns;

    public FieldLayoutRow(final FieldDivider fieldDivider, final FieldColumnLayout[] columns) {
        this.fieldDivider = fieldDivider;
        this.columns = columns;
    }

    public FieldDivider getFieldDivider() {
        return fieldDivider;
    }

    public FieldColumnLayout[] getColumns() {
        return columns;
    }
}
