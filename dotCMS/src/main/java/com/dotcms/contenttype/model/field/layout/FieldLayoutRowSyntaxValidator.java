package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import java.util.List;

/**
 * Run throught a {@link FieldLayout} and allow validate it.
 */
abstract class FieldLayoutRowSyntaxValidator {
    private final List<Field> fields;

    protected FieldLayoutRowSyntaxValidator(final List<Field> fields) {
        this.fields = fields;
    }

    /**
     * Check if a {@link FieldLayout} is valid.
     *
     * @throws FieldLayoutValidationException when a {@link FieldLayout} is not valid
     */
    public final void validate() throws FieldLayoutValidationException {
        if (this.fields.isEmpty()) {
            processEmptyFields();
        } else {
            for (final FieldUtil.FieldsFragment fragment : FieldUtil.splitByFieldDivider(fields)) {
                final boolean tabDivider = FieldUtil.isTabDivider(fragment.getFieldDivider());

                if (tabDivider) {
                    processTab((TabDividerField) fragment.getFieldDivider());

                    if (fragment.hasOthersFields()) {
                        processRow(fragment.getOthersFields());
                        processColumns(fragment.getOthersFields());
                    }
                } else {
                    processRow(fragment.getAllFields());
                    processColumns(fragment.getOthersFields());
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
    protected final void processColumns (final List<Field> columnsFields)
            throws FieldLayoutValidationException {

        if (columnsFields.isEmpty()) {
            processColumn(columnsFields);
        } else {
            for (final List<Field> columnFields : FieldUtil.splitByColumnField(columnsFields)) {
                processColumn(columnFields);
            }
        }
    }

    /**
     * Process a {@link TabDividerField} into a row in a {@link FieldLayout}
     *
     * @param tabDividerField {@link TabDividerField}
     * @throws FieldLayoutValidationException when the {@link TabDividerField}  is not valid
     */
    protected abstract void processTab (final TabDividerField tabDividerField) throws FieldLayoutValidationException;

    /**
     * Process a row in a {@link FieldLayout}
     *
     * @param rowFields fields into the row
     * @throws FieldLayoutValidationException when the row is not valid
     */
    protected abstract void processRow (final List<Field> rowFields) throws FieldLayoutValidationException;

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
     * CCall when the List os fields is empty, each concrete class can decide if throw a exception or not in that case
     *
     * @throws
     */
    protected abstract void processEmptyFields() throws FieldLayoutValidationException;
}
