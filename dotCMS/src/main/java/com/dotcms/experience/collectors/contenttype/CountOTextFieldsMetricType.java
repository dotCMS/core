package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOTextFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.TextField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_TEXT_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of text fields";
    }
}
