package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfJSONFieldsMetricType extends ContentTypeFieldsMetricType {
    @Override
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.JSONField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_JSON_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of JSON fields";
    }
}
