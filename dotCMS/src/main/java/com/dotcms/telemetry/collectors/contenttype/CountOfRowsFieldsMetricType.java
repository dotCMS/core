package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfRowsFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.RowField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_ROW_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of row fields";
    }
}
