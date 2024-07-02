package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.util.diff.DiffCommand;
import com.dotcms.util.diff.DiffItem;
import com.dotcms.util.diff.DiffResult;
import com.dotcms.util.diff.Differentiator;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Implements the diff command for fields
 * @author jsanca
 */
public class FieldDiffCommand implements DiffCommand<FieldDiffItemsKey, Field ,String, Field> {

    private final String contentTypeId;
    private final Differentiator<Field> fieldDifferentiator;
    private final Differentiator<Map<String, FieldVariable>> fieldVariableDifferentiator;

    public FieldDiffCommand(final String contentTypeId) {
        this.contentTypeId = contentTypeId;
        this.fieldDifferentiator = this::diff;
        this.fieldVariableDifferentiator = this::diffFieldVariable;
    }

    public FieldDiffCommand(final String contentTypeId,
            final Differentiator<Field> fieldDifferentiator,
            final Differentiator<Map<String, FieldVariable>> fieldVariableDifferentiator) {

        this.contentTypeId = contentTypeId;
        this.fieldDifferentiator = fieldDifferentiator;
        this.fieldVariableDifferentiator = fieldVariableDifferentiator;
    }

    @Override
    public DiffResult<FieldDiffItemsKey, Field> applyDiff(final Map<String, Field> currentObjects,
                                               final Map<String, Field> newObjects) {

        final DiffResult.Builder<FieldDiffItemsKey, Field> builder = new DiffResult.Builder<>();

        final Map<FieldDiffItemsKey, Field> fieldsToDelete = currentObjects.entrySet().stream()
                .filter(entry -> findField(newObjects.values(), entry.getValue()).isEmpty())
                .collect(Collectors.toMap(
                        entry ->
                                new FieldDiffItemsKey(
                                        entry.getKey(),
                                        new DiffItem.Builder().variable(entry.getKey()).build()
                                ),
                        entry -> ensureFieldContentTypeId(contentTypeId, entry.getValue())
                ));

        final Map<FieldDiffItemsKey, Field> fieldsToAdd = newObjects.entrySet().stream()
                .filter(entry -> findField(currentObjects.values(), entry.getValue()).isEmpty())
                .collect(Collectors.toMap(
                        entry ->
                                new FieldDiffItemsKey(
                                        entry.getKey(),
                                        new DiffItem.Builder().variable(entry.getKey()).build()
                                ),
                        entry -> ensureFieldContentTypeId(contentTypeId, entry.getValue())
                ));

        final Map<FieldDiffItemsKey, Field> fieldsToUpdate = new HashMap<>();
        for (final Map.Entry<String, Field> entry : newObjects.entrySet()) {

            final var foundFieldOptional = findField(currentObjects.values(), entry.getValue());
            if (foundFieldOptional.isPresent()) {

                final Collection<DiffItem> diffItems = this.fieldDifferentiator.diff(
                        foundFieldOptional.get(), entry.getValue()
                );

                if (UtilMethods.isSet(diffItems)) {
                    fieldsToUpdate.put(
                            new FieldDiffItemsKey(entry.getKey(), diffItems),
                            ensureFieldContentTypeId(contentTypeId, entry.getValue())
                    );
                }
            }
        }

        return builder.putAllToDelete(fieldsToDelete).
                putAllToAdd(fieldsToAdd).putAllToUpdate(fieldsToUpdate).build();
    }

    /**
     * Find a field in a collection of fields. It tries to find the field by id first, and if it is
     * not found, it tries to find the field by variable.
     *
     * @param fields   the collection of fields to search
     * @param toSearch the field to be found
     * @return an Optional containing the found field, or an empty Optional if the field is not
     * found
     */
    private Optional<Field> findField(final Collection<Field> fields, final Field toSearch) {

        for (final Field field : fields) {

            // Trying first with id
            if ((StringUtils.isNotEmpty(field.id()) && StringUtils.isNotEmpty(toSearch.id()))
                    && field.id().equals(toSearch.id())) {
                return Optional.of(field);
            }

            // Trying with the variable
            if ((StringUtils.isNotEmpty(field.variable())
                    && StringUtils.isNotEmpty(toSearch.variable()))
                    && field.variable().equalsIgnoreCase(toSearch.variable())) {
                return Optional.of(field);
            }

        }

        return Optional.empty();
    }

    private Collection<DiffItem> diff(final Field field1, final Field field2) {

        final List<DiffItem> diffItems = new ArrayList<>();

        if ((null != field1.dataType() && field1.dataType() != field2.dataType()) || null == field1.dataType() && null != field2.dataType()) {

            diffItems.add(new DiffItem.Builder().variable("dataType").message(field1.dataType() + " != " + field2.dataType()).build());
        }

        if (field1.fixed() != field2.fixed())  {

            diffItems.add(new DiffItem.Builder().variable("fixed").message(field1.fixed() + " != " + field2.fixed()).build());
        }

        if (field1.indexed() != field2.indexed())  {

            diffItems.add(new DiffItem.Builder().variable("indexed").message(field1.indexed() + " != " + field2.indexed()).build());
        }

        if (field1.listed() != field2.listed())  {

            diffItems.add(new DiffItem.Builder().variable("listed").message(field1.listed() + " != " + field2.listed()).build());
        }

        if (field1.readOnly() != field2.readOnly())  {

            diffItems.add(new DiffItem.Builder().variable("readOnly").message(field1.readOnly() + " != " + field2.readOnly()).build());
        }

        if (field1.legacyField() != field2.legacyField())  {

            diffItems.add(new DiffItem.Builder().variable("legacyField").message(field1.legacyField() + " != " + field2.legacyField()).build());
        }

        if (field1.required() != field2.required())  {

            diffItems.add(new DiffItem.Builder().variable("required").message(field1.required() + " != " + field2.required()).build());
        }

        if (field1.searchable() != field2.searchable())  {

            diffItems.add(new DiffItem.Builder().variable("searchable").message(field1.searchable() + " != " + field2.searchable()).build());
        }

        if (field1.unique() != field2.unique())  {

            diffItems.add(new DiffItem.Builder().variable("unique").message(field1.unique() + " != " + field2.unique()).build());
        }

        if (this.diff(field1.defaultValue(), field2.defaultValue()))  {

            diffItems.add(new DiffItem.Builder().variable("defaultValue").message(field1.defaultValue() + " != " + field2.defaultValue()).build());
        }

        if (this.diff(field1.name(), field2.name()))  {

            diffItems.add(new DiffItem.Builder().variable("name").message(field1.name() + " != " + field2.name()).build());
        }

        if (this.diff(field1.hint(), field2.hint()))  {

            diffItems.add(new DiffItem.Builder().variable("hint").message(field1.hint() + " != " + field2.hint()).build());
        }

        if (this.diff(field1.owner(), field2.owner()))  {

            diffItems.add(new DiffItem.Builder().variable("owner").message(field1.owner() + " != " + field2.owner()).build());
        }

        if (this.diff(field1.regexCheck(), field2.regexCheck()))  {

            diffItems.add(new DiffItem.Builder().variable("regexCheck").message(field1.regexCheck() + " != " + field2.regexCheck()).build());
        }

        if (this.diff(field1.values(), field2.values()))  {

            diffItems.add(new DiffItem.Builder().variable("values").message(field1.values() + " != " + field2.values()).build());
        }

        if (this.diff(field1.relationType(), field2.relationType()))  {

            diffItems.add(new DiffItem.Builder().variable("relationType").message(field1.relationType() + " != " + field2.relationType()).build());
        }

        final Map<String, FieldVariable> fieldVariablesMap1 = UtilMethods.get(this.indexFieldVariables(field1), Collections::emptyMap);
        final Map<String, FieldVariable> fieldVariablesMap2 = UtilMethods.get(this.indexFieldVariables(field2), Collections::emptyMap);
        final boolean areFieldVariablesEmpty = fieldVariablesMap1.isEmpty() && fieldVariablesMap2.isEmpty();

        if (!areFieldVariablesEmpty) {
            diffItems.addAll(this.fieldVariableDifferentiator.diff(fieldVariablesMap1, fieldVariablesMap2));
        }

        return diffItems;
    }

    final Map<String, FieldVariable> indexFieldVariables (final Field field) {

        final Map<String, FieldVariable> fmap = new HashMap<>();
        for (final FieldVariable fv : field.fieldVariables()) {
            fmap.put(fv.key(), fv);
        }
        return fmap;
    }

    private Collection<DiffItem> diffFieldVariable(final Map<String, FieldVariable> currentFieldVariablesMap,
                         final Map<String, FieldVariable> newFieldVariablesMap) {

        final List<DiffItem> diffItems = new ArrayList<>();

        final Collection<DiffItem> fieldsVariablesToDelete = currentFieldVariablesMap.entrySet().stream()
                .filter(entry ->  !newFieldVariablesMap.containsKey(entry.getKey()))
                .map(entry -> new DiffItem.Builder().variable("fieldVariable." + entry.getKey())
                        .detail("delete").build())
                .collect(Collectors.toList());

        final Collection<DiffItem>  fieldsVariablesToAdd   = newFieldVariablesMap.entrySet().stream()
                .filter(entry ->  !currentFieldVariablesMap.containsKey(entry.getKey()))
                .map(entry -> new DiffItem.Builder().variable("fieldVariable." + entry.getKey())
                        .detail("add").build())
                .collect(Collectors.toList());

        final Collection<DiffItem> fieldsVariablesToUpdate = newFieldVariablesMap.entrySet().stream()
                .filter(entry ->  currentFieldVariablesMap.containsKey(entry.getKey()))
                .filter(entry ->  !entry.getValue().value().equals(currentFieldVariablesMap.get(entry.getKey()).value()))
                .map(entry -> new DiffItem.Builder().variable("fieldVariable." + entry.getKey())
                        .detail("update").message(entry.getValue().value() + " != " + currentFieldVariablesMap.get(entry.getKey()).value()).build())
                .collect(Collectors.toList());

        diffItems.addAll(fieldsVariablesToDelete);
        diffItems.addAll(fieldsVariablesToAdd);
        diffItems.addAll(fieldsVariablesToUpdate);

        return diffItems;
    }

    private boolean diff(final String s1, final String s2) {

        return (null != s1 && !s1.equals(s2)) || null == s1 && null != s2;
    }

    /**
     * Ensures that the content type id is set on the given field. If the content type id is not set
     * or is different from the provided content type id, a new Field object is created with the
     * updated content type id and returned. Otherwise, the original field is returned.
     *
     * @param contentTypeId the desired content type id
     * @param field         the field to be checked and updated if necessary
     * @return the field object with the updated content type id, or the original field if no update
     * is needed
     */
    private Field ensureFieldContentTypeId(final String contentTypeId, final Field field) {

        // Make sure the content type id is set on the field
        if (StringUtils.isEmpty(field.contentTypeId()) ||
                !field.contentTypeId().equals(contentTypeId)) {
            return FieldBuilder.builder(field).contentTypeId(contentTypeId).build();
        }

        return field;
    }

}
