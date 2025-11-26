package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldDivider;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Util class that provide method to handle {@link Field}
 */
public class FieldUtil {

    public static class SortOrderFix {
        private List<Field> newFields;
        private List<Field> updatedFields;

        private SortOrderFix(final List<Field> newFields, final List<Field> updatedFields) {
            this.newFields = newFields;
            this.updatedFields = updatedFields;
        }

        public List<Field> getNewFields() {
            return newFields;
        }

        public List<Field> getUpdatedFields() {
            return updatedFields;
        }
    }

    private FieldUtil() {}

    /**
     * Split a set of fields by {@link FieldDivider}, for example if we have to follow set of fields:
     *
     * <pre>
     * [
     *      RowField(id=1),
     *      ColumnField(id=2),
     *      TextField(id=3),
     *      TabDividerField(id=4),
     *      RowField(id=5),
     *      ColumnField(id=6),
     *      TextField(id=7),
     *      TextField(id=8)
     * ]
     * </pre>
     *
     * this method return the follow:
     *
     * <ul>
     *     <li>
     *         fieldDivider: RowField(id=1)
     *         othersFields; [ColumnField(id=2), TextField(id=3)]
     *     </li>
     *
     *     <li>
     *         fieldDivider: TabDividerField(id=4)
     *         othersFields; []
     *     </li>
     *
     *     <li>
     *         fieldDivider: RowField(id=5)
     *         othersFields; [ColumnField(id=6), TextField(id=7), TextField(id=8)]
     *     </li>
     * </ul>
     *
     * @param fields
     * @return
     */
    public static List<FieldsFragment> splitByFieldDivider(final Collection<Field> fields) {
        return split(fields, FieldDivider.class)
                .stream()
                .map(FieldsFragment::new)
                .collect(Collectors.toList());
    }

    /**
     * Split a set of fields by {@link ColumnField}, for example if we have to follow set of fields:
     *
     * <pre>
     * [
     *      ColumnField(id=1),
     *      TextField(id=2),
     *      ColumnField(id=3),
     *      TextField(id=4),
     *      TextField(id=5)
     * ]
     * </pre>
     *
     * this method return the follow:
     *
     * <pre>
     * [
     *      [
     *        ColumnField(id=1),
     *        TextField(id=2),
     *        ColumnField(id=3)
     *      ],
     *      [
     *        TextField(id=4),
     *        TextField(id=5)
     *      ]
     * ]
     * </pre>
     *
     * @param fields
     * @return
     */
    public static List<List<Field>> splitByColumnField(final Collection<Field> fields) {
        return split(fields, ColumnField.class);
    }

    private static List<List<Field>> split(final Collection<Field> fields, final Class<?> divider) {
        final List<List<Field>> fieldDividersSplit = new ArrayList<>();
        List<Field> currentFieldDivider = new ArrayList<>();

        for (final Field field : fields) {
            if (divider.isAssignableFrom(field.getClass()) && !currentFieldDivider.isEmpty()) {
                fieldDividersSplit.add(currentFieldDivider);
                currentFieldDivider = new ArrayList<>();
            }

            if (currentFieldDivider != null) {
                currentFieldDivider.add(field);
            }
        }

        if (!currentFieldDivider.isEmpty()) {
            fieldDividersSplit.add(currentFieldDivider);
        }

        return fieldDividersSplit;
    }

    /**
     * Return true if the field implement {@link FieldDivider}, otherwise return false
     *
     * @param field
     * @return
     */
    static boolean isFieldDivider(final Field field) {
        return isType(field, FieldDivider.class);
    }

    /**
     * Return true if the field is a {@link RowField}, otherwise return false
     *
     * @param field
     * @return
     */
    static boolean isRowDivider(final Field field) {
        return isType(field, RowField.class);
    }

    /**
     * Return true if the field is a {@link TabDividerField}, otherwise return false
     *
     * @param field
     * @return
     */
    static boolean isTabDivider(final Field field) {
        return isType(field, TabDividerField.class);
    }

    /**
     * Return true if the field is a {@link ColumnField}, otherwise return false
     *
     * @param field
     * @return
     */
    static boolean isColumnField(final Field field) {
        return isType(field, ColumnField.class);
    }

    private static boolean isType(final Field field, final Class clazz) {
        return field != null &&  clazz.isAssignableFrom(field.getClass());
    }

    /**
     * Set the {@link Field#sortOrder()} to be equals the fields list index
     *
     * @param fields new List with all sort order equals to list's index
     * @return a new {@link SortOrderFix} with the ordered fields
     */
    public static SortOrderFix fixSortOrder(final List<Field> fields) {
        final List<Field> newFieldsWithSorOrder = new ArrayList<>();
        final List<Field> fieldsUpdated = new ArrayList<>();

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);

            if (i != field.sortOrder()) {
                final Field fieldCopy = copyField(field, i);
                newFieldsWithSorOrder.add(fieldCopy);
                fieldsUpdated.add(fieldCopy);
            } else {
                newFieldsWithSorOrder.add(field);
            }
        }

        return new SortOrderFix(newFieldsWithSorOrder, fieldsUpdated);
    }

    /**
     * Copy a Field changing the sortOrder attribute
     *
     * @param field Field to copy
     * @param newSortOrder new sort order
     * @return new field with all the attributes's valus equal to field but sortorder
     */
    public static Field copyField(final Field field, final int newSortOrder) {
        JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);
        final JSONObject jsonObject = jsonFieldTransformer.jsonObject();

        try {
            jsonObject.put("sortOrder", newSortOrder);
        } catch (JSONException e) {
            Logger.error(FieldUtil.class, String.format("Eror witg fiels %s: %s", field.name(), e.getMessage()));
            throw new DotRuntimeException(e);
        }

        jsonFieldTransformer = new JsonFieldTransformer(jsonObject.toString());
        return jsonFieldTransformer.from();
    }

    /**
     * Represent a fragment from a set of fields split by {@link FieldDivider}
     */
    public static class FieldsFragment {
        private Field fieldDivider;
        private List<Field> othersFields;

        public FieldsFragment(final List<Field> fields) {
            if (!fields.isEmpty() && FieldUtil.isFieldDivider(fields.get(0))) {
                this.fieldDivider = fields.get(0);
                this.othersFields = fields.subList(1, fields.size());
            } else {
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
