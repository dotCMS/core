package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfBinaryFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.BinaryField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_BINARY_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of binary fields";
    }
}
