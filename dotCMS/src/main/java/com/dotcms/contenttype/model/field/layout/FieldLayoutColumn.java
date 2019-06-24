package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.util.CollectionsUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

/**
 * Represent a column into a {@link FieldLayoutRow}
 *
 * @see FieldLayout
 */
@JsonSerialize(using = FieldLayoutColumnSerializer.class)
public class FieldLayoutColumn {
    @JsonProperty("columnDivider")
    private final ColumnField column;
    private final List<Field> fields;

    public FieldLayoutColumn(final ColumnField column, final List<Field> fields) {
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

    /**
     * Return the fields into the column plus the ColumnField in the first position
     * @return
     */
    @JsonIgnore
    public List<Field> getAllFields() {

        return new ImmutableList.Builder<Field>()
                .add(column)
                .addAll(CollectionsUtils.asList(fields.iterator()))
                .build();
    }
}
