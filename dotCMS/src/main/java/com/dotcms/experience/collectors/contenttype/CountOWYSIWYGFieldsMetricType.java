package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOWYSIWYGFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.WysiwygField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_WYSIWYG_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of WYSIWYG fields";
    }
}
