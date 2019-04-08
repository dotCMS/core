package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

abstract class FieldLayoutRowSyntaxValidator {
    private final List<Field> fields;

    protected FieldLayoutRowSyntaxValidator(final List<Field> fields) {
        this.fields = fields;
    }

    public final void validate() throws FieldLayoutValidationException {
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


    protected final void processColumns (final List<Field> columns)
            throws FieldLayoutValidationException {

        if (columns.isEmpty()) {
            processColumn(columns);
        } else {
            for (final List<Field> columnFields : FieldUtil.splitByColumnField(columns)) {
                processColumn(columnFields);
            }
        }
    }

    protected abstract void processTab (final TabDividerField tabDividerField) throws FieldLayoutValidationException;

    protected abstract void processRow (final List<Field> fragmentFields) throws FieldLayoutValidationException;

    protected abstract void processColumn (final List<Field> columnFields) throws FieldLayoutValidationException;

    protected abstract void processSortOrder(final List<Field> fields) throws FieldLayoutValidationException;
}
