package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TabDividerField;

import java.util.List;

/**
 *  Strict Field Layout Validator, throw a {@link FieldLayoutValidationException} when a {@link FieldLayout} is not valid
 *
 * @see FieldLayoutRowSyntaxValidator#validate()
 */
public class StrictFieldLayoutRowSyntaxValidator extends FieldLayoutRowSyntaxValidator {

    StrictFieldLayoutRowSyntaxValidator(final List<Field> fields) {
        super(fields);
    }

    @Override
    protected List<List<Field>> processMaxColumnsRule(final List<List<Field>> columns)
            throws FieldLayoutValidationException {

        throw new FieldLayoutValidationException(
                String.format("Max columns by rows exceeded: %s", columns));
    }

    @Override
    protected void processNotEmptyRow (final List<Field> fragmentFields)
            throws FieldLayoutValidationException {

        final Field firstField = fragmentFields.isEmpty() ? null : fragmentFields.get(0);

        if (firstField == null || !FieldUtil.isRowDivider(firstField)) {
            throw new FieldLayoutValidationException(
                    String.format("Expected RowField or TabField before: %s %s", fragmentFields, firstField));
        }
    }

    @Override
    protected void processEmptyRow (final Field rowField) throws FieldLayoutValidationException {
        throw new FieldLayoutValidationException(String.format("Empty row is not allow"));
    }

    @Override
    protected void processColumn (final List<Field> columnFields)
            throws FieldLayoutValidationException {
        final Field firstField = columnFields.isEmpty() ? null : columnFields.get(0);

        if (firstField == null || !FieldUtil.isColumnField(firstField)) {
            throw new FieldLayoutValidationException(String.format("Expected ColumnField before: %s", columnFields));
        }
    }

    @Override
    protected  void processTab (final TabDividerField tabDividerField) {
        // DO nothing
    }

    @Override
    protected void processSortOrder(final List<Field> fields) throws FieldLayoutValidationException {
        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);

            if (field.sortOrder() != i) {
                throw new FieldLayoutValidationException(String.format("sortOrder is not right for %s", field));
            }
        }
    }

    @Override
    protected void processEmptyFields() {
        // DO nothing
    }
}
