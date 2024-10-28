package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOfImageFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.ImageField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_IMAGE_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of image fields";
    }
}
