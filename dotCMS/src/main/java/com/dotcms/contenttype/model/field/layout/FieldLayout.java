package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldDivider;
import com.dotcms.contenttype.model.field.RowField;
import com.dotmarketing.exception.DotRuntimeException;
import org.apache.commons.lang.reflect.FieldUtils;

import java.util.*;
import java.util.stream.Collectors;


public class FieldLayout {
    private final List<Field> fields;
    private NotStrictFieldLayoutRowSyntaxValidator notStrictFieldLayoutRowSyntaxValidator;
    private StrictFieldLayoutRowSyntaxValidator strictFieldLayoutRowSyntaxValidator;

    public FieldLayout(final Collection<Field> fields) {
        this.fields = new ArrayList<>(fields);
        this.fields.sort(Comparator.comparingInt(Field::sortOrder));
    }

    public List<FieldLayoutRow> getRows() {
        return FieldUtil.splitByFieldDivider(this.getFields())
            .stream()
            .map((final FieldUtil.FieldsFragment fragment) ->
                new FieldLayoutRow (
                    (FieldDivider) fragment.getFieldDivider(),
                    FieldUtil.splitByColumnField(fragment.getOthersFields())
                            .stream()
                            .map((final List<Field> rowFields) ->
                                    new FieldLayoutColumn(
                                            (ColumnField) rowFields.get(0),
                                            rowFields.subList(1, rowFields.size())
                                    )
                            )
                            .collect(Collectors.toList())
                )
            )
            .collect(Collectors.toList());
    }

    public List<Field> getFields() {

        if (notStrictFieldLayoutRowSyntaxValidator == null) {
            notStrictFieldLayoutRowSyntaxValidator = new NotStrictFieldLayoutRowSyntaxValidator(this.fields);
        }

        try {
            notStrictFieldLayoutRowSyntaxValidator.validate();
            return notStrictFieldLayoutRowSyntaxValidator.getFields();
        } catch (FieldLayoutValidationException e) {
            throw new DotRuntimeException(e);
        }
    }

    public void validate() throws FieldLayoutValidationException {
        if (strictFieldLayoutRowSyntaxValidator == null) {
            strictFieldLayoutRowSyntaxValidator = new StrictFieldLayoutRowSyntaxValidator(this.fields);
        }
        
        strictFieldLayoutRowSyntaxValidator.validate();
    }

    public FieldLayout update (final List<Field> fieldsToUpdate) {
        final List<Field> newFields = new ArrayList<>(fields);

        fieldsToUpdate
            .stream()
            .forEach(field -> {
                if (!Objects.isNull(field.id())) {
                    remove(newFields, field);
                }

                final int newIndex = field.sortOrder() < newFields.size() ? field.sortOrder() : newFields.size();
                newFields.add(newIndex, field);
            });

        final List<Field> fieldsOrdered = FieldUtil.fixSortOrder(newFields);
        return new FieldLayout(fieldsOrdered);
    }

    private void remove(final List<Field> newFields, final Field field) {
        final Iterator<Field> fieldsIterator = newFields.iterator();

        while (fieldsIterator.hasNext()) {
            final Field next = fieldsIterator.next();
            if (next.id() != null && next.id().equals(field.id())) {
                fieldsIterator.remove();
                break;
            }
        }
    }

    public FieldLayout remove (final List<String> fieldsIdToRemove) {
        final List<Field> newFields = new ArrayList<>(fields);
        final Iterator<Field> fieldIterator = newFields.iterator();

        while(fieldIterator.hasNext()) {
            final Field field = fieldIterator.next();

            if (fieldsIdToRemove.contains(field.id())) {
                fieldIterator.remove();
            }
        }
        final List<Field> fieldsOrdered = FieldUtil.fixSortOrder(newFields);
        return new FieldLayout(fieldsOrdered);
    }

}
