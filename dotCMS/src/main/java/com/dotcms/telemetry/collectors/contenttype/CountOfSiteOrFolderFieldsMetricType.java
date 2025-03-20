package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfSiteOrFolderFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.HostFolderField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_SITE_OR_FOLDER_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of site or folder fields";
    }
}
