package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountODateFieldsMetricType extends ContentTypeFieldsMetricType {
    @Override
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.DateField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_DATE_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of date fields";
    }
}
