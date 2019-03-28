package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RowField;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FieldLayoutTest {

    @Test
    public void shouldCreateFieldLayout() {
        final Field[] fields = {
            mock(RowField.class),
            mock(ColumnField.class),
            mock(Field.class),
            mock(RowField.class),
            mock(ColumnField.class),
            mock(Field.class),
            mock(Field.class),
            mock(ColumnField.class),
            mock(Field.class),
        };

        final FieldLayout fieldLayout = new FieldLayout(fields);
        final FieldLayoutRow[] rows = fieldLayout.getRows();

        assertEquals(2, rows.length);
        //assertEquals(1, rows[0].getColumns().length);
        //assertEquals(1, rows[0].getColumns()[0].getFields().length);
        //assertEquals(2, rows[1].getColumns().length);
        //assertEquals(2, rows[0].getColumns()[1].getFields().length);
        //assertEquals(0, rows[0].getColumns()[2].getFields().length);
        //assertArrayEquals(fields, fieldLayout.getFields());
    }
}
