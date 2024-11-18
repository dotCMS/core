package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfRelationshipFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.RelationshipField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_RELATIONSHIP_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of relationship fields";
    }
}
