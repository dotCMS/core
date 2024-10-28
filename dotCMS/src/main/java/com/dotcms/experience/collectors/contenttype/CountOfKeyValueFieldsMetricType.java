package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOfKeyValueFieldsMetricType extends ContentTypeFieldsMetricType {
    @Override
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.KeyValueField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_KEY_VALUE_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of key/value fields";
    }
}
