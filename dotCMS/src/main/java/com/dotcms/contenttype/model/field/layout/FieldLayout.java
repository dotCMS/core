package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;

import java.util.Arrays;
import java.util.Comparator;

public class FieldLayout {
    final FieldLayoutRow[] rows = null;

    public FieldLayout(final Field[] fields) {
        /*Arrays.stream(fields)
            .sorted(new Comparator<Field>() {
                @Override
                public int compare(final Field field1, final Field field2) {
                    return field1.sortOrder() - field2.sortOrder();
                }
            })
            .map();*/
    }

    public Field[] getFields() {
        return null;
    }

    public FieldLayoutRow[] getRows() {
        return rows;
    }
}
