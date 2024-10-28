package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOfColumnsFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.ColumnField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_COLUMN_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of column fields";
    }
}
