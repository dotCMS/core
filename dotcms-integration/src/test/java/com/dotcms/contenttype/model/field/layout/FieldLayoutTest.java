package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.util.CollectionsUtils;

import com.dotcms.util.IntegrationTestInitService;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldLayoutTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldCreateFieldLayoutWhenHasMoreThanOneTabDividerInRow() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(TabDividerField.class),
                mock(TabDividerField.class),
                mock(TabDividerField.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
        assertEquals(fields, fieldLayout.getFields());
    }

    @Test
    public void shouldCreateFieldLayoutAndNotThrowException() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
            mock(RowField.class),
            mock(ColumnField.class),
            mock(Field.class),
            mock(RowField.class),
            mock(ColumnField.class),
            mock(Field.class),
            mock(Field.class),
            mock(ColumnField.class),
            mock(Field.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
        assertEquals(fields, fieldLayout.getFields());
    }

    @Test
    public void shouldCreateFieldLayoutWithTabDivider() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(TabDividerField.class),
                mock(RowField.class),
                mock(ColumnField.class),
                mock(Field.class),
                mock(TabDividerField.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
        assertEquals(fields, fieldLayout.getFields());
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowWhenSortOrderIsNorRight() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(TabDividerField.class),
                mock(RowField.class),
                mock(ColumnField.class),
                mock(Field.class),
                mock(TabDividerField.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i * 2);
        }

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowFieldLayoutValidationExceptionWhenHasRowWithoutColumn()
            throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(RowField.class),
                mock(RowField.class),
                mock(Field.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowFieldLayoutValidationExceptionWhenHasRowsWithoutColumn()
            throws FieldLayoutValidationException {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(RowField.class),
                mock(RowField.class),
                mock(RowField.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowFieldLayoutValidationExceptionWhenHasFieldsWithoutRows()
            throws FieldLayoutValidationException {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(Field.class),
                mock(Field.class),
                mock(Field.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowFieldLayoutValidationExceptionWhenHasTabFieldWithoutRows()
            throws FieldLayoutValidationException {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(TabDividerField.class),
                mock(ColumnField.class),
                mock(Field.class),
                mock(Field.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowFieldLayoutValidationExceptionWhenHasColumnWithoutRow()
            throws FieldLayoutValidationException {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                mock(ColumnField.class),
                mock(Field.class),
                mock(Field.class)
        );

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            when(field.sortOrder()).thenReturn(i);
        }

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        fieldLayout.validate();
    }

    @Test()
    public void shouldFixLayoutWhenHasRowWithoutColumn() {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                ImmutableRowField.builder()
                        .name("Row Field 1")
                        .sortOrder(0)
                        .build(),
                ImmutableRowField.builder()
                        .name("Row Field 2")
                        .sortOrder(1)
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final List<Field> fieldsLayout = fieldLayout.getFields();

        assertEquals(3, fieldsLayout.size());
        assertEquals(fields.get(1).name(), fieldsLayout.get(0).name());
        assertEquals(ImmutableColumnField.class, fieldsLayout.get(1).getClass());
        assertEquals(fields.get(2).name(), fieldsLayout.get(2).name());
    }

    @Test()
    public void shouldFixLayoutValidationExceptionWhenHasFieldsWithoutRows() {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
            ImmutableTextField.builder()
                    .name("Text Field 1")
                    .sortOrder(0)
                    .id("0")
                    .build(),
            ImmutableTextField.builder()
                    .name("Text Field 2")
                    .sortOrder(1)
                    .id("1")
                    .build(),
            ImmutableTextField.builder()
                    .name("Text Field 3")
                    .sortOrder(2)
                    .id("2")
                    .build()
        );

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final List<Field> fieldsLayout = fieldLayout.getFields();

        assertEquals(5, fieldsLayout.size());
        assertEquals(ImmutableRowField.class, fieldsLayout.get(0).getClass());
        assertEquals(ImmutableColumnField.class, fieldsLayout.get(1).getClass());
        assertEquals(fields.get(0).name(), fieldsLayout.get(2).name());
        assertEquals(fields.get(1).name(), fieldsLayout.get(3).name());
        assertEquals(fields.get(2).name(), fieldsLayout.get(4).name());
    }

    @Test()
    public void shouldFixLayoutValidationExceptionWhenHasTabFieldWithoutRows() {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                ImmutableTabDividerField.builder()
                        .name("Tab Divider 1")
                        .sortOrder(0)
                        .build(),
                ImmutableColumnField.builder()
                        .name("Column Field 1")
                        .sortOrder(1)
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field 1")
                        .sortOrder(2)
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field 2")
                        .sortOrder(3)
                        .build()
        );


        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final List<Field> fieldsLayout = fieldLayout.getFields();

        assertEquals(5, fieldsLayout.size());
        assertEquals(fields.get(0), fieldsLayout.get(0));
        assertEquals(ImmutableRowField.class, fieldsLayout.get(1).getClass());
        assertEquals(fields.get(1).name(), fieldsLayout.get(2).name());
        assertEquals(fields.get(2).name(), fieldsLayout.get(3).name());
        assertEquals(fields.get(3).name(), fieldsLayout.get(4).name());
    }

    @Test()
    public void shouldFixLayoutValidationExceptionWhenHasColumnWithoutRow() {

        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
            ImmutableColumnField.builder()
                    .name("Column Field 1")
                    .sortOrder(0)
                    .build(),
            ImmutableTextField.builder()
                    .name("Text Field 1")
                    .sortOrder(1)
                    .build(),
            ImmutableTextField.builder()
                    .name("Text Field 2")
                    .sortOrder(2)
                    .id("2")
                    .build()
        );

        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final List<Field> fieldsLayout = fieldLayout.getFields();

        assertEquals(4, fieldsLayout.size());
        assertEquals(ImmutableRowField.class, fieldsLayout.get(0).getClass());
        assertEquals(fields.get(0).name(), fieldsLayout.get(1).name());
        assertEquals(fields.get(1).name(), fieldsLayout.get(2).name());
        assertEquals(fields.get(2).name(), fieldsLayout.get(3).name());
    }

    @Test()
    public void shouldFixSortOrder() {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = CollectionsUtils.list(
                ImmutableRowField.builder()
                        .name("Row Field")
                        .sortOrder(4)
                        .build(),
                ImmutableColumnField.builder()
                        .name("Column Field")
                        .sortOrder(6)
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(7)
                        .build()
        );


        FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final List<Field> fieldsLayout = fieldLayout.getFields();

        for(int i = 0; i < fieldsLayout.size(); i++) {
            assertEquals(i, fieldsLayout.get(i).sortOrder());
        }
    }

    @Test
    public void shouldRemoveFieldsAndNotThrowException() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final FieldLayout fieldLayoutRemove = fieldLayout.remove(CollectionsUtils.list("2", "6"));
        fieldLayoutRemove.validate();

        final List<Field> fieldsExpected = CollectionsUtils.list(
                fields.get(0),
                fields.get(1),
                fields.get(3),
                fields.get(4),
                fields.get(5),
                fields.get(7),
                fields.get(8)
        );

        assertFieldListEquals(fieldsExpected, fieldLayoutRemove.getFields());
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldRemoveFieldsAndThrowException() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final FieldLayout fieldLayoutRemove = fieldLayout.remove(CollectionsUtils.list("4"));
        fieldLayoutRemove.validate();
    }

    @Test
    public void shouldMoveFieldBackward() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);

        final List<Field> fieldsToUpdate = CollectionsUtils.list(
                ImmutableTextField.builder()
                        .name("Text Field 1")
                        .sortOrder(8)
                        .id("2")
                        .build()
        );

        final FieldLayout fieldLayoutRemove = fieldLayout.update(fieldsToUpdate);
        fieldLayoutRemove.validate();

        final List<Field> fieldsExpected = CollectionsUtils.list(
                fields.get(0),
                fields.get(1),
                fields.get(3),
                fields.get(4),
                fields.get(5),
                fields.get(6),
                fields.get(7),
                fields.get(8),
                fields.get(2)
        );

        assertFieldListEquals(FieldUtil.fixSortOrder(fieldsExpected).getNewFields(), fieldLayoutRemove.getFields());
    }

    @Test
    public void shouldMoveFieldForward() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);

        final List<Field> fieldsToUpdate = CollectionsUtils.list(
                ImmutableTextField.builder()
                        .name("Text Field 4")
                        .sortOrder(2)
                        .id("8")
                        .build()
        );

        final FieldLayout fieldLayoutUpdated = fieldLayout.update(fieldsToUpdate);
        fieldLayoutUpdated.validate();

        final List<Field> fieldsExpected = CollectionsUtils.list(
                fields.get(0),
                fields.get(1),
                fields.get(8),
                fields.get(2),
                fields.get(3),
                fields.get(4),
                fields.get(5),
                fields.get(6),
                fields.get(7)
        );

        assertFieldListEquals(FieldUtil.fixSortOrder(fieldsExpected).getNewFields(), fieldLayoutUpdated.getFields());
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldMoveFieldAndThrowException() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);

        final List<Field> fieldsToUpdate = CollectionsUtils.list(
                ImmutableColumnField.builder()
                        .name("Column Field")
                        .sortOrder(0)
                        .id("1")
                        .build()
        );

        final FieldLayout fieldLayoutUpdated = fieldLayout.update(fieldsToUpdate);
        fieldLayoutUpdated.validate();
    }

    @Test()
    public void shouldAddNewField() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);

        final ImmutableTextField newField = ImmutableTextField.builder()
                .name("New Text Field")
                .sortOrder(2)
                .build();

        final List<Field> fieldsToUpdate = CollectionsUtils.list(newField);

        final FieldLayout fieldLayoutUpdated = fieldLayout.update(fieldsToUpdate);
        fieldLayoutUpdated.validate();

        final List<Field> fieldsExpected = CollectionsUtils.list(
                fields.get(0),
                fields.get(1),
                newField,
                fields.get(2),
                fields.get(3),
                fields.get(4),
                fields.get(5),
                fields.get(6),
                fields.get(7),
                fields.get(8)
        );

        assertFieldListEquals(fieldsExpected, fieldLayoutUpdated.getFields());

    }

    private void assertFieldListEquals(final List<Field> fields1, final List<Field> fields2) {
        assertEquals(
                FieldUtil.fixSortOrder(fields1).getNewFields().stream().map(Field::name).collect(Collectors.toList()),
                FieldUtil.fixSortOrder(fields2).getNewFields().stream().map(Field::name).collect(Collectors.toList())
        );
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldAddNewFieldAndThrowException() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);

        final ImmutableTextField newField = ImmutableTextField.builder()
                .name("Text Field")
                .sortOrder(1)
                .build();

        final List<Field> fieldsToUpdate = CollectionsUtils.list(newField);

        final FieldLayout fieldLayoutRemove = fieldLayout.update(fieldsToUpdate);
        fieldLayoutRemove.validate();
    }

    @Test
    public void shouldCreateFieldLayout() throws FieldLayoutValidationException {
        final ContentType contentType = mock(ContentType.class);
        final List<Field> fields = this.getData();

        final FieldLayout fieldLayout = new FieldLayout(contentType, fields);
        final List<FieldLayoutRow> rows = fieldLayout.getRows();

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());
        assertEquals(1, rows.get(0).getColumns().get(0).getFields().size());
        assertEquals(2, rows.get(1).getColumns().size());
        assertEquals(2, rows.get(1).getColumns().get(0).getFields().size());
        assertEquals(1, rows.get(1).getColumns().get(1).getFields().size());
    }

    @Test()
    public void shouldreturnContentTypeInternationalizationWhenItIsSet() {

        final ContentType contentType = mock(ContentType.class);

        final List<Field> fields = CollectionsUtils.list(
                ImmutableRowField.builder()
                        .name("Row Field 1")
                        .sortOrder(0)
                        .build(),
                ImmutableRowField.builder()
                        .name("Row Field 2")
                        .sortOrder(1)
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );
        when(contentType.fields()).thenReturn(fields);

        final ContentTypeInternationalization contentTypeInternationalization = mock(ContentTypeInternationalization.class);

        final FieldLayout fieldLayout = new FieldLayout(contentType);
        fieldLayout.setContentTypeInternationalization(contentTypeInternationalization);

        assertEquals(contentTypeInternationalization, fieldLayout.getContentTypeInternationalization());
    }

    @Test()
    public void shouldReturnNullContentTypeInternationalizationWhenItIsNotSet() {

        final ContentType contentType = mock(ContentType.class);

        final List<Field> fields = CollectionsUtils.list(
                ImmutableRowField.builder()
                        .name("Row Field 1")
                        .sortOrder(0)
                        .build(),
                ImmutableRowField.builder()
                        .name("Row Field 2")
                        .sortOrder(1)
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );
        when(contentType.fields()).thenReturn(fields);
        
        final FieldLayout fieldLayout = new FieldLayout(contentType);

        assertNull(fieldLayout.getContentTypeInternationalization());
    }

    @NotNull
    private List<Field> getData() {
        return CollectionsUtils.list(
                ImmutableRowField.builder()
                        .name("Row Field 1")
                        .sortOrder(0)
                        .id("0")
                        .build(),
                ImmutableColumnField.builder()
                        .name("Column Field 1")
                        .sortOrder(1)
                        .id("1")
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field 1")
                        .sortOrder(2)
                        .id("2")
                        .build(),
                ImmutableRowField.builder()
                        .name("Row Field 2")
                        .sortOrder(3)
                        .id("3")
                        .build(),
                ImmutableColumnField.builder()
                        .name("Column Field 2")
                        .sortOrder(4)
                        .id("4")
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field 2")
                        .sortOrder(5)
                        .id("5")
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field 3")
                        .sortOrder(6)
                        .id("6")
                        .build(),
                ImmutableColumnField.builder()
                        .name("Column Field 3")
                        .sortOrder(7)
                        .id("7")
                        .build(),
                ImmutableTextField.builder()
                        .name("Text Field 4")
                        .sortOrder(8)
                        .id("8")
                        .build()
        );
    }
}
