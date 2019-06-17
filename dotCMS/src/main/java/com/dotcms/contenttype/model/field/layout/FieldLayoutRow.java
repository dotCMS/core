package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldDivider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotcms.util.CollectionsUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represent a row into a {@link FieldLayoutRow}, a row could be represent by a
 * {@link com.dotcms.contenttype.model.field.RowField} or a {@link com.dotcms.contenttype.model.field.TabDividerField}
 *
 * @see FieldLayout
 */
@JsonSerialize(using = FieldLayoutRowSerializer.class)
public class FieldLayoutRow {
    private final Field divider;
    private final List<FieldLayoutColumn> columns;

    public FieldLayoutRow(final Field fieldDivider, final List<FieldLayoutColumn> columns) {
        this.divider = fieldDivider;
        this.columns = columns;
    }

    /**
     * Return the {@link FieldDivider}
     *
     * @return
     */
    public Field getDivider() {
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

    /**
     * Return the fields into the column plus the {@link com.dotcms.contenttype.model.field.RowField} in the first position
     * @return
     */
    @JsonIgnore
    public List<Field> getAllFields() {

        return new ImmutableList.Builder<Field>()
                .add((Field) divider)
                .addAll(this.getAllColumnsFields())
                .build();
    }

    private Iterable<? extends Field> getAllColumnsFields() {
        return columns.stream()
            .flatMap(column -> column.getAllFields().stream())
            .collect(Collectors.toList());
    }
}
