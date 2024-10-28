package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOPermissionsFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.PermissionTabField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_PERMISSIONS_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of permissions fields";
    }
}
