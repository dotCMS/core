package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;

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

    private List<Field> newFields = new ArrayList<>();

    NotStrictFieldLayoutRowSyntaxValidator(List<Field> fields) {
        super(fields);
    }

    /**
     * return {@link this#newFields}
     *
     * @return
     */
    public List<Field> getFields() {
        return newFields;
    }

    /**
     * If the first element in fragmentFields is null or is not a {@link FieldDivider} then it create a new {@link RowField}
     * and add it to {@link this#newFields}
     *
     * @param fragmentFields
     */
    @Override
    protected void processRow (final List<Field> fragmentFields) {
        final Field firstField = fragmentFields.isEmpty() ? null : fragmentFields.get(0);

        if (firstField == null || !FieldUtil.isFieldDivider(firstField)) {
            newFields.add(getNewRowField());
        } else {
            newFields.add(firstField);
        }
    }

    /**
     * If the first element in fragmentFields is null or is not a {@link FieldDivider} then it create a new {@link ColumnField}
     * and add it to {@link this#newFields}
     *
     * @param columnFields fields into the column
     */
    protected void processColumn (final List<Field> columnFields) {
        final Field firstField = columnFields.isEmpty() ? null : columnFields.get(0);

        if (firstField == null || !FieldUtil.isColumnField(firstField)) {
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
        this.newFields = FieldUtil.fixSortOrder(this.newFields);
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
                .build();
    }

    private RowField getNewRowField() {
        return ImmutableRowField.builder()
                .name("Row Field")
                .build();
    }
}
