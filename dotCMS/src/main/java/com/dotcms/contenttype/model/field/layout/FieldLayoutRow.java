package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.FieldDivider;
import com.dotcms.util.CollectionsUtils;
import java.util.List;

public class FieldLayoutRow {
    private final FieldDivider fieldDivider;
    private final List<FieldLayoutColumn> columns;

    FieldLayoutRow(final FieldDivider fieldDivider, final List<FieldLayoutColumn> columns) {
        this.fieldDivider = fieldDivider;
        this.columns = columns;
    }

    public FieldDivider getFieldDivider() {
        return fieldDivider;
    }

    public List<FieldLayoutColumn> getColumns() {
        return CollectionsUtils.asList(columns.iterator());
    }
}
