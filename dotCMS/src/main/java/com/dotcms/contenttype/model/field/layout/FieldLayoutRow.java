package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.FieldDivider;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotcms.util.CollectionsUtils;

import java.util.List;

@JsonSerialize(using = FieldLayoutRowSerializer.class)
public class FieldLayoutRow {
    private final FieldDivider divider;
    private final List<FieldLayoutColumn> columns;

    FieldLayoutRow(final FieldDivider fieldDivider, final List<FieldLayoutColumn> columns) {
        this.divider = fieldDivider;
        this.columns = columns;
    }

    public FieldDivider getDivider() {
        return divider;
    }

    public List<FieldLayoutColumn> getColumns() {
        return CollectionsUtils.asList(columns.iterator());
    }
}
