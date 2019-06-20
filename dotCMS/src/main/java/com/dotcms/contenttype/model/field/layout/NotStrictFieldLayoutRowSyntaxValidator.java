package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Check if the {@link FieldLayout} fulfill a right sintax and if it is not then fix it doing:
 *
 * <ul>
 *     <li>
 *         If exists a {@link RowField} that if not follow by {@link ColumnField} then crete the {@link ColumnField}.
 *     </li>
 *     <li>
 *         If exists any {@link Field}  different to  {@link RowField}, {@link ColumnField} or
 *     {@link com.dotcms.contenttype.model.field.TabDividerField} without {@link ColumnField} then crete the
 *     {@link RowField} and the {@link ColumnField}.
 *     </li>
 *     <li>
 *         If the {@link Field#sortOrder()} is wrong for any Field then fix it
 *     </li>
 * </ul>
 *
 * @see FieldLayout
 */
public class NotStrictFieldLayoutRowSyntaxValidator extends FieldLayoutRowSyntaxValidator {

    private ContentType contentType;
    private List<Field> newFields;
    private List<Field> layoutFieldsToRemove;
    private List<Field> layoutFieldsToUpdate;

    NotStrictFieldLayoutRowSyntaxValidator(final ContentType contentType, final List<Field> fields) {
        super(fields);

        this.contentType = contentType;
    }

    @Override
    protected  void beforeStartValidate() {
        newFields = new ArrayList<>();
        layoutFieldsToRemove = new ArrayList<>();
        layoutFieldsToUpdate = new ArrayList<>();
    }

    @Override
    protected List<List<Field>> processMaxColumnsRule(final List<List<Field>> columns) {
        final List<List<Field>> newColumns = new ArrayList<>();

        for(int i = 0; i < columns.size(); i++) {
            if (i < MAX_NUM_COLUMNS_ALLOW) {
                final List<Field> column = new ArrayList<>();
                column.addAll(columns.get(i));
                newColumns.add(column);
            } else if (!columns.get(i).isEmpty()){
                final List<Field> withoutColumnField = columns.get(i).subList(1, columns.get(i).size());
                newColumns.get(MAX_NUM_COLUMNS_ALLOW - 1).addAll(withoutColumnField);

                final Field columnToRemove = columns.get(i).get(0);
                layoutFieldsToRemove.add(columnToRemove);
            }

        }

        return newColumns;
    }

    /**
     * return {@link this#newFields}
     *
     * @return
     */
    public List<Field> getFields() {
        return newFields;
    }

    public List<Field> getFieldsToRemove() {
        return layoutFieldsToRemove;
    }

    public List<Field> getLayoutFieldsToCreateOrUpdate()  {
        return layoutFieldsToUpdate;
    }

    /**
     * If the first element in fragmentFields is null or is not a {@link FieldDivider} then it create a new {@link RowField}
     * and add it to {@link this#newFields}
     *
     * @param rowFields
     */
    @Override
    protected void processNotEmptyRow (final List<Field> rowFields) {
        final Field firstField = rowFields.isEmpty() ? null : rowFields.get(0);

        if (!FieldUtil.isFieldDivider(firstField)) {
            newFields.add(getNewRowField());
        } else {
            newFields.add(firstField);
        }
    }

    /**
     * Always ignore empty row
     */
    @Override
    protected void processEmptyRow (final Field rowField) {
        layoutFieldsToRemove.add(rowField);
    }

    /**
     * If the first element in fragmentFields is null or is not a {@link FieldDivider} then it create a new {@link ColumnField}
     * and add it to {@link this#newFields}
     *
     * @param columnFields fields into the column
     */
    protected void processColumn (final List<Field> columnFields) {
        final Field firstField = columnFields.isEmpty() ? null : columnFields.get(0);

        if (!FieldUtil.isColumnField(firstField)) {
            newFields.add(getNewColumnField());
        }

        newFields.addAll(columnFields);
    }

    /**
     * Add the tabDividerField to {@link this#newFields}
     *
     * @param tabDividerField {@link TabDividerField}
     */
    @Override
    protected  void processTab (final TabDividerField tabDividerField) {
        newFields.add(tabDividerField);
    }

    /**
     * Check if the fields's sort order are right and if not then set them to equal the list index.
     *
     * @param fields All the field into the layout
     */
    @Override
    protected void processSortOrder(final List<Field> fields) {
        final FieldUtil.SortOrderFix sortOrderFix = FieldUtil.fixSortOrder(this.newFields);
        this.newFields = sortOrderFix.getNewFields();
        this.layoutFieldsToUpdate = sortOrderFix.getUpdatedFields();
    }

    /**
     * Create a {@link RowField} and a {@link ColumnField} and add them to {@link this#newFields}
     */
    @Override
    protected void processEmptyFields()  {
        newFields.add(getNewRowField());
        newFields.add(getNewColumnField());
    }

    private ColumnField getNewColumnField() {
        return ImmutableColumnField.builder()
                .name("Column Field")
                .contentTypeId(this.contentType.id())
                .build();
    }

    private RowField getNewRowField() {
        return ImmutableRowField.builder()
                .name("Row Field")
                .contentTypeId(this.contentType.id())
                .build();
    }
}
