package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class FieldUtil {
    static List<FieldLayoutRowSyntaxValidator.FieldsFragment> splitByFieldDivider(final Collection<Field> fields) {
        return split(fields, FieldDivider.class)
                .stream()
                .map(FieldLayoutRowSyntaxValidator.FieldsFragment::new)
                .collect(Collectors.toList());
    }

    static List<List<Field>> splitByColumnField(final Collection<Field> fields) {
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

    static boolean isFieldDivider(final Field field) {
        return isType(field, FieldDivider.class);
    }

    static boolean isRowDivider(final Field field) {
        return isType(field, RowField.class);
    }

    static boolean isTabDivider(final Field field) {
        return isType(field, TabDividerField.class);
    }

    static boolean isColumnField(final Field field) {
        return isType(field, ColumnField.class);
    }

    private static boolean isType(final Field field, Class clazz) {
        return field != null &&  clazz.isAssignableFrom(field.getClass());
    }

    static List<Field> fixSortOrder(final List<Field> fields) {
        final List<Field> newFieldsWithSorOrder = new ArrayList<>();

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);

            if (i != field.sortOrder()) {
                final Field fieldCopy = copyField(field, i);
                newFieldsWithSorOrder.add(fieldCopy);
            } else {
                newFieldsWithSorOrder.add(field);
            }
        }

        return newFieldsWithSorOrder;
    }

    private static Field copyField(final Field field, final int newSortOrder) {
        JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);
        final JSONObject jsonObject = jsonFieldTransformer.jsonObject();

        try {
            jsonObject.put("sortOrder", newSortOrder);
        } catch (JSONException e) {
            throw new DotRuntimeException(e);
        }

        jsonFieldTransformer = new JsonFieldTransformer(jsonObject.toString());
        return jsonFieldTransformer.from();
    }
}
