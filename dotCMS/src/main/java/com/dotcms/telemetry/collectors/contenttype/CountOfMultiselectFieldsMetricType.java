package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfMultiselectFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.MultiSelectField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_MULTI_SELECT_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of multi select fields";
    }
}
