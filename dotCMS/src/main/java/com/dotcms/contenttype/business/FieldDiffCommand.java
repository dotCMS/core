package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.util.diff.DiffCommand;
import com.dotcms.util.diff.DiffResult;
import com.dotcms.util.diff.Differentiator;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements the diff command for fields
 * @author jsanca
 */
public class FieldDiffCommand implements DiffCommand<String, Field> {

    private final Differentiator<Field> fieldDifferentiator;

    public FieldDiffCommand() {
        this.fieldDifferentiator = this::diff;
    }

    public FieldDiffCommand(final Differentiator<Field> fieldDifferentiator) {
        this.fieldDifferentiator = fieldDifferentiator;
    }

    @Override
    public DiffResult<String, Field> applyDiff(final Map<String, Field> currentObjects,
                                               final Map<String, Field> newObjects) {

        final DiffResult.Builder<String, Field> builder = new DiffResult.Builder<>();

        final Map<String, Field> fieldsToDelete = currentObjects.entrySet().stream()
                .filter(entry ->  !newObjects.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

        final Map<String, Field> fieldsToAdd    = newObjects.entrySet().stream()
                .filter(entry ->  !currentObjects.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

        final Map<String, Field> fieldsToUpdate = newObjects.entrySet().stream()
                .filter(entry -> currentObjects.containsKey(entry.getKey())
                        && this.fieldDifferentiator.diff(entry.getValue(), currentObjects.get(entry.getKey())))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

        return builder.putAllToDelete(fieldsToDelete).
                putAllToAdd(fieldsToAdd).putAllToUpdate(fieldsToUpdate).build();
    }

    private boolean diff(final Field field1, final Field field2) {

        // todo: double check this
        return ((null != field1.dataType() && field1.dataType() != field2.dataType()) || null == field1.dataType() && null != field2.dataType()) ||

                (field1.fixed() != field2.fixed()) ||
                (field1.indexed() != field2.indexed()) ||
                (field1.listed() != field2.listed()) ||
                (field1.readOnly() != field2.readOnly()) ||
                (field1.legacyField() != field2.legacyField()) ||
                (field1.required() != field2.required()) ||
                (field1.searchable() != field2.searchable()) ||
                (field1.unique() != field2.unique()) ||

                this.diff(field1.defaultValue(), field2.defaultValue()) ||
                this.diff(field1.name(), field2.name()) ||
                this.diff(field1.id(), field2.id()) ||
                this.diff(field1.hint(), field2.hint()) ||
                this.diff(field1.owner(), field2.owner()) ||
                this.diff(field1.regexCheck(), field2.regexCheck()) ||
                this.diff(field1.values(), field2.values()) ||
                this.diff(field1.relationType(), field2.relationType()) ||
                this.diff(field1.relationType(), field2.relationType());
    }

    private boolean diff(final String s1, final String s2) {

        return (null != s1 && !s1.equals(s2)) || null == s1 && null != s2;
    }
}
