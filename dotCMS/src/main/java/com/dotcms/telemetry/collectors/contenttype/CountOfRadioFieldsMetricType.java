package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;

public class CountOfRadioFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.RadioField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_RADIO_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of radio fields";
    }
}
