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
        for (final FieldsFragment fragment : FieldUtil.splitByFieldDivider(fields)) {
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

    protected static class FieldsFragment {
        private Field fieldDivider;
        private List<Field> othersFields;

        public FieldsFragment(final List<Field> fields) {
            if (!fields.isEmpty() && FieldUtil.isFieldDivider(fields.get(0))) {
                this.fieldDivider = fields.get(0);
                this.othersFields = fields.subList(1, fields.size());
            } else {
                this.fieldDivider = null;
                this.othersFields = fields;
            }
        }

        public Field getFieldDivider() {
            return fieldDivider;
        }

        public List<Field> getOthersFields() {
            return othersFields;
        }

        public List<Field> getAllFields() {
            final List fields = new ArrayList();

            if (fieldDivider != null) {
                fields.add(fieldDivider);
            }

            fields.addAll(othersFields);
            return fields;
        }

        @Override
        public String toString() {
            return "FieldsFragment{" +
                    "fieldDivider=" + fieldDivider +
                    ", othersFields=" + othersFields +
                    '}';
        }

        public boolean hasOthersFields() {
            return !this.othersFields.isEmpty();
        }
    }
}
