package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotcms.util.CollectionsUtils;

import java.util.Collections;
import java.util.List;

/**
 * Represent a column into a {@link FieldLayoutRow}
 *
 * @see FieldLayout
 */
@JsonSerialize(using = FieldLayoutColumnSerializer.class)
public class FieldLayoutColumn {
    private final ColumnField column;
    private final List<Field> fields;

    FieldLayoutColumn(final ColumnField column, final List<Field> fields) {
        this.column = column;
        this.fields = fields == null ? Collections.EMPTY_LIST : fields;
    }

    /**
     * Return the {@link ColumnField}
     * @return
     */
    public ColumnField getColumn() {
        return column;
    }

    /**
     * Return the fields into the column
     * @return
     */
    public List<Field> getFields() {
        return CollectionsUtils.asList(fields.iterator());
    }
}
