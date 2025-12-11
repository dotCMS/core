package com.dotcms.telemetry.collectors.contenttype;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class CountOfConstantFieldsMetricType extends ContentTypeFieldsMetricType {
    @Override
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.ConstantField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_CONSTANT_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of constant fields";
    }

    @Override
    public String getDisplayLabel() {
        return "Constant Fields";
    }
}
