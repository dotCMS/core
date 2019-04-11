package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotStrictFieldLayoutRowSyntaxValidator extends FieldLayoutRowSyntaxValidator {

    private List<Field> newFields = new ArrayList<>();

    NotStrictFieldLayoutRowSyntaxValidator(List<Field> fields) {
        super(fields);
    }

    public List<Field> getFields() {
        return newFields;
    }

    @Override
    protected void processRow (final List<Field> fragmentFields) {
        final Field firstField = fragmentFields.isEmpty() ? null : fragmentFields.get(0);

        if (firstField == null || !FieldUtil.isFieldDivider(firstField)) {
            newFields.add(getNewRowField());
        } else {
            newFields.add(firstField);
        }
    }

    protected void processColumn (final List<Field> columnFields) {
        final Field firstField = columnFields.isEmpty() ? null : columnFields.get(0);

        if (firstField == null || !FieldUtil.isColumnField(firstField)) {
            newFields.add(getNewColumnField());
        }

        newFields.addAll(columnFields);
    }

    @Override
    protected  void processTab (final TabDividerField tabDividerField) {
        newFields.add(tabDividerField);
    }

    @Override
    protected void processSortOrder(final List<Field> fields) {
        this.newFields = FieldUtil.fixSortOrder(this.newFields);
    }

    @Override
    protected void processEmptyFields()  {
        newFields.add(getNewRowField());
        newFields.add(getNewColumnField());
    }

    private ColumnField getNewColumnField() {
        return ImmutableColumnField.builder()
                .name("Column Field")
                .build();
    }

    private RowField getNewRowField() {
        return ImmutableRowField.builder()
                .name("Row Field")
                .build();
    }
}
