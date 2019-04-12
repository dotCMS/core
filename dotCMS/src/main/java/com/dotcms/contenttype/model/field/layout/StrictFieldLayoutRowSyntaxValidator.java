package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TabDividerField;

import java.util.List;

public class StrictFieldLayoutRowSyntaxValidator extends FieldLayoutRowSyntaxValidator {

    StrictFieldLayoutRowSyntaxValidator(List<Field> fields) {
        super(fields);
    }

    @Override
    protected void processRow (final List<Field> fragmentFields)
            throws FieldLayoutValidationException {

        final Field firstField = fragmentFields.isEmpty() ? null : fragmentFields.get(0);

        if (firstField == null || !FieldUtil.isRowDivider(firstField)) {
            throw new FieldLayoutValidationException(
                    String.format("Expected RowField or TabField before: %s %s", fragmentFields, firstField));
        }
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
