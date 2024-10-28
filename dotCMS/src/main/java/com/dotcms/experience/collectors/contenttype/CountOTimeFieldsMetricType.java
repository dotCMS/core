package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOTimeFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.TimeField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_TIME_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of time fields";
    }
}
