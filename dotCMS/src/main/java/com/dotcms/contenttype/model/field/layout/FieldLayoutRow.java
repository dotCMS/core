package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.FieldDivider;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotcms.util.CollectionsUtils;

import java.util.List;

/**
 * Represent a row into a {@link FieldLayoutRow}, a row could be represent by a
 * {@link com.dotcms.contenttype.model.field.RowField} or a {@link com.dotcms.contenttype.model.field.TabDividerField}
 *
 * @see FieldLayout
 */
@JsonSerialize(using = FieldLayoutRowSerializer.class)
public class FieldLayoutRow {
    private final FieldDivider divider;
    private final List<FieldLayoutColumn> columns;

    FieldLayoutRow(final FieldDivider fieldDivider, final List<FieldLayoutColumn> columns) {
        this.divider = fieldDivider;
        this.columns = columns;
    }

    /**
     * Return the {@link FieldDivider}
     *
     * @return
     */
    public FieldDivider getDivider() {
        return divider;
    }

    /**
     * Return the columns into the row
     *
     * @return
     */
    public List<FieldLayoutColumn> getColumns() {
        return CollectionsUtils.asList(columns.iterator());
    }
}
