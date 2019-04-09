package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotcms.util.CollectionsUtils;
import java.util.List;

@JsonSerialize(using = FieldLayoutColumnSerializer.class)
public class FieldLayoutColumn {
    private final ColumnField column;
    private final List<Field> fields;

    FieldLayoutColumn(final ColumnField column, final List<Field> fields) {
        this.column = column;
        this.fields = fields;
    }

    public ColumnField getColumn() {
        return column;
    }

    public List<Field> getFields() {
        return CollectionsUtils.asList(fields.iterator());
    }
}
