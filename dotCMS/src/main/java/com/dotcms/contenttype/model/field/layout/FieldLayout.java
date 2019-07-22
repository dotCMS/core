package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * It represent the layout for a set of fields, this set of fields is sort by {@link Field#sortOrder()} and must to fulfill
 * with the follow:
 *
 * <ul>
 *     <li>Each {@link RowField} have to be follow by at least one {@link ColumnField}</li>
 *     <li>Before any {@link ColumnField} must exist one {@link RowField}</li>
 *     <li>Before any {@link Field}  different to  {@link RowField}, {@link ColumnField} or
 *     {@link com.dotcms.contenttype.model.field.TabDividerField} must exist at least one {@link ColumnField} before it</li>
 * </ul>
 *
 * For Example the follow sintax are right:
 *
 * <ul>
 *     <li>RowField, ColumnField, TextField</li>
 *     <li>TabDividerField, RowField, ColumnField, TextField</li>
 *     <li>RowField, ColumnField, TextField, ColumnField</li>
 *     <li>RowField, ColumnField, TextField, RowField, ColumnField, TextField, TextField</li>
 * </ul>
 *
 * For Example the follow sintax are wrong:
 *
 * <ul>
 *     <li><b>ColumnField, TextField</b>: Always before any {@link ColumnField} have to exist a {@link RowField}</li>
 *     <li><b>TabDividerField, RowField, TextField</b>: before any {@link Field} have to exist at least one {@link ColumnField}</li>
 *     <li><b>ColumnField, TextField, ColumnField</b>: before any {@link ColumnField} have to exists at least one RowField</li>
 * </ul>
 *
 * @see FieldLayoutColumn
 * @see FieldLayoutRow
 */
@JsonSerialize(using = FieldLayoutSerializer.class)
public class FieldLayout {
    private final ContentType contentType;
    private final List<Field> fields;
    private NotStrictFieldLayoutRowSyntaxValidator notStrictFieldLayoutRowSyntaxValidator;
    private StrictFieldLayoutRowSyntaxValidator strictFieldLayoutRowSyntaxValidator;
    private ContentTypeInternationalization contentTypeInternationalization;

    /**
     *
     * @param fields set of fields to build the layout
     */
    public FieldLayout(
            final ContentType contentType,
            final Collection<Field> fields) {

        this.fields = new ArrayList<>(fields);
        this.fields.sort(Comparator.comparingInt(Field::sortOrder));
        this.contentType = contentType;
    }

    public FieldLayout(final ContentType contentType) {

        this(contentType, contentType.fields());
    }

    /**
     * Return the rows for a layout, each row can be represent by a {@link com.dotcms.contenttype.model.field.TabDividerField}
     * or by a {@link RowField}, if the layout doesn't have the right sintax this method fix it.
     *
     * @return {@link FieldLayoutRow} array
     *
     * @see NotStrictFieldLayoutRowSyntaxValidator
     */
    public List<FieldLayoutRow> getRows() {
        return FieldUtil.splitByFieldDivider(this.getFields())
            .stream()
            .map((final FieldUtil.FieldsFragment fragment) ->
                new FieldLayoutRow (
                    fragment.getFieldDivider(),
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

    /**
     * Return all the fields sort by {@link Field#sortOrder()}, this array contains the {@link RowField}, {@link ColumnField}
     * and {@link com.dotcms.contenttype.model.field.TabDividerField}.
     * For example if you have a layout with one rows and two columns and a {@link com.dotcms.contenttype.model.field.TextField}
     * in each columns then you'll have the follow array:
     *
     * {RowField, ColumnField, TextField, ColumnField, TextField}
     *
     * @return
     */
    public List<Field> getFields() {

        try {
            if (notStrictFieldLayoutRowSyntaxValidator == null) {
                notStrictFieldLayoutRowSyntaxValidator = new NotStrictFieldLayoutRowSyntaxValidator(this.contentType, this.fields);
                notStrictFieldLayoutRowSyntaxValidator.validate();
            }


            return notStrictFieldLayoutRowSyntaxValidator.getFields();
        } catch (FieldLayoutValidationException e) {
            Logger.error(FieldLayout.class, e.getMessage());
            throw new DotRuntimeException(e);
        }
    }

    /**
     * List of Layout fields to remove to get a right layout,
     * Layout field could be: {@link RowField} and  {@link ColumnField}
     * @return
     */
    public List<Field>  getLayoutFieldsToDelete() {
        try {
            if (notStrictFieldLayoutRowSyntaxValidator == null) {
                notStrictFieldLayoutRowSyntaxValidator = new NotStrictFieldLayoutRowSyntaxValidator(this.contentType, this.fields);
                notStrictFieldLayoutRowSyntaxValidator.validate();
            }

            return notStrictFieldLayoutRowSyntaxValidator.getFieldsToRemove();
        } catch (FieldLayoutValidationException e) {
            Logger.error(FieldLayout.class, e.getMessage());
            throw new DotRuntimeException(e);
        }
    }

    /**
     * List of Layout fields to create or Update to get a right layout,
     * Layout field could be: {@link RowField} and  {@link ColumnField}
     * @return
     */
    public List<Field>  getLayoutFieldsToCreateOrUpdate() {
        try {
            if (notStrictFieldLayoutRowSyntaxValidator == null) {
                notStrictFieldLayoutRowSyntaxValidator = new NotStrictFieldLayoutRowSyntaxValidator(this.contentType, this.fields);
                notStrictFieldLayoutRowSyntaxValidator.validate();
            }

            return notStrictFieldLayoutRowSyntaxValidator.getLayoutFieldsToCreateOrUpdate();
        } catch (FieldLayoutValidationException e) {
            Logger.error(FieldLayout.class, e.getMessage());
            throw new DotRuntimeException(e);
        }
    }

    /**
     * CHeck if the layout fulfill all the sintax rules
     *
     * @throws FieldLayoutValidationException when any sintax error is found in the layout
     */
    public void validate() throws FieldLayoutValidationException {
        if (strictFieldLayoutRowSyntaxValidator == null) {
            strictFieldLayoutRowSyntaxValidator = new StrictFieldLayoutRowSyntaxValidator(this.fields);
        }
        
        strictFieldLayoutRowSyntaxValidator.validate();
    }

    /**
     * Retrn true is the FieldLayout is valid
     * @throws FieldLayoutValidationException
     */
    public boolean isValidate() {
        if (strictFieldLayoutRowSyntaxValidator == null) {
            strictFieldLayoutRowSyntaxValidator = new StrictFieldLayoutRowSyntaxValidator(this.fields);
        }

        try {
            strictFieldLayoutRowSyntaxValidator.validate();
            return true;
        } catch (FieldLayoutValidationException e) {
            return false;
        }
    }

    /**
     * Update the layout and crete a new layout with the change
     *
     * @param fieldsToUpdate fields to update, they could be new o existing field,
     *                       this fields'll be insert according to its {@link Field#sortOrder()}.
     * @return
     */
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

        final List<Field> fieldsOrdered = FieldUtil.fixSortOrder(newFields).getNewFields();
        return new FieldLayout(this.contentType, fieldsOrdered);
    }

    /**
     * Remove a set of fields from the layout and return a new layout with the change.
     * @param fieldsIdToRemove ids of the fields to remove
     * @return
     */
    public FieldLayout remove (final List<String> fieldsIdToRemove) {
        final List<Field> newFields = new ArrayList<>(fields);
        final Iterator<Field> fieldIterator = newFields.iterator();

        while(fieldIterator.hasNext()) {
            final Field field = fieldIterator.next();

            if (fieldsIdToRemove.contains(field.id())) {
                fieldIterator.remove();
            }
        }
        final List<Field> fieldsOrdered = FieldUtil.fixSortOrder(newFields).getNewFields();
        return new FieldLayout(this.contentType, fieldsOrdered);
    }

    private void remove(final List<Field> newFields, final Field field) {
        newFields.removeIf(next -> next.id() != null && next.id().equals(field.id()));
    }

    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Get a fixed Layout from this
     * @return
     */
    public FieldLayout getLayoutFixed() {
        return new FieldLayout(contentType, this.getFields());
    }

    public ContentTypeInternationalization getContentTypeInternationalization() {
        return contentTypeInternationalization;
    }

    public void setContentTypeInternationalization(final ContentTypeInternationalization contentTypeInternationalization) {
        this.contentTypeInternationalization = contentTypeInternationalization;
    }
}
