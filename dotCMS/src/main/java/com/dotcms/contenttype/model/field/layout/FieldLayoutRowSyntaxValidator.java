package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import java.util.List;

/**
 * Run throught a {@link FieldLayout} and allow validate it.
 */
abstract class FieldLayoutRowSyntaxValidator {
    protected static final int MAX_NUM_COLUMNS_ALLOW = 4;
    private final List<Field> fields;

    protected FieldLayoutRowSyntaxValidator(final List<Field> fields) {
        this.fields = fields;
    }

    /**
     * Check if a {@link FieldLayout} is valid.
     * @see FieldLayout to know when a FieldLayout is not valid
     *
     * @throws FieldLayoutValidationException when a {@link FieldLayout} is not valid
     */
    public final void validate() throws FieldLayoutValidationException {
        beforeStartValidate();

        if (this.fields.isEmpty()) {
            processEmptyFields();
        } else {
            for (final FieldUtil.FieldsFragment fragment : FieldUtil.splitByFieldDivider(fields)) {
                final boolean tabDivider = FieldUtil.isTabDivider(fragment.getFieldDivider());

                if (tabDivider) {
                    processTab((TabDividerField) fragment.getFieldDivider());

                    if (fragment.hasOthersFields()) {
                        processAnyRow(new FieldUtil.FieldsFragment(fragment.getOthersFields()));
                    }
                } else {
                    processAnyRow(fragment);
                }
            }

            processSortOrder(fields);
        }
    }

    /**
     * Process all the columns into a row in a {@link FieldLayout}
     *
     * @param columnsFields columns's fields
     * @throws FieldLayoutValidationException when the any column is not valid
     *
     * @see {@link this#processColumn(List)}
     */
    private void processColumns (final List<Field> columnsFields)
            throws FieldLayoutValidationException {

        if (columnsFields.isEmpty()) {
            processColumn(columnsFields);
        } else {
            List<List<Field>> columns = FieldUtil.splitByColumnField(columnsFields);

            if (columns.size() > MAX_NUM_COLUMNS_ALLOW) {
                columns = processMaxColumnsRule(columns);
            }

            for (final List<Field> columnFields : columns) {
                processColumn(columnFields);
            }
        }
    }

    /**
     * Process a row in a {@link FieldLayout}
     *
     * @param fragment row's fields
     * @throws FieldLayoutValidationException when the row is not valid
     */
    private void processAnyRow (final FieldUtil.FieldsFragment fragment) throws FieldLayoutValidationException {
        if (fragment.getOthersFields().isEmpty()) {
            processEmptyRow(fragment.getFieldDivider());
        } else {
            processNotEmptyRow(fragment.getAllFields());
            processColumns(fragment.getOthersFields());
        }
    }

    /**
     *
     * @param columns
     * @return
     */
    protected abstract List<List<Field>> processMaxColumnsRule(List<List<Field>> columns) throws FieldLayoutValidationException;

    /**
     * Process a {@link TabDividerField} into a row in a {@link FieldLayout}
     *
     * @param tabDividerField {@link TabDividerField}
     * @throws FieldLayoutValidationException when the {@link TabDividerField}  is not valid
     */
    protected abstract void processTab (final TabDividerField tabDividerField) throws FieldLayoutValidationException;

    /**
     * Allow process a empty row
     *
     * @throws FieldLayoutValidationException
     */
    protected abstract void processEmptyRow (final Field rowField) throws FieldLayoutValidationException;

    /**
     * Allow Process a not empty row
     *
     * @param rowFields
     * @throws FieldLayoutValidationException
     */
    protected abstract void processNotEmptyRow (final List<Field> rowFields) throws FieldLayoutValidationException;

    /**
     * Process a column into a row.
     *
     * @param columnFields fields into the column
     * @throws FieldLayoutValidationException when the column is not valid
     */
    protected abstract void processColumn (final List<Field> columnFields) throws FieldLayoutValidationException;

    /**
     * Check if the {@link Field#sortOrder()} is right.
     *
     * @param fields All the field into the layout
     * @throws FieldLayoutValidationException when the {@link Field#sortOrder()} is not right.
     */
    protected abstract void processSortOrder(final List<Field> fields) throws FieldLayoutValidationException;

    /**
     * Call when the List os fields is empty, each concrete class can decide if throw a exception or not in that case
     *
     * @throws
     */
    protected abstract void processEmptyFields() throws FieldLayoutValidationException;

    protected  void beforeStartValidate() {}
}
