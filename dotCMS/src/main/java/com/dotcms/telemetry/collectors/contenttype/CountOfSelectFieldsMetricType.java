package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountOfSelectFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.SelectField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_SELECT_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of select fields";
    }
}
