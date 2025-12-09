package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountOfCategoryFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.CategoryField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_CATEGORY_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of category fields";
    }
}
