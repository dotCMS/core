package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class CountOfCheckboxFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.CheckboxField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_CHECKBOX_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of checkbox fields";
    }

    @Override
    public String getDisplayLabel() {
        return "Checkbox Fields";
    }
}
