package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOfTabFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.TabDividerField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_TAB_DIVIDER_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of tab divider fields";
    }
}
