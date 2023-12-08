package com.dotcms.analytics.experience.metric;

import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonValue;


public enum MetricFeature {
    WORKFLOWS;

    @JsonValue
    public String getCamelCaseName()  {
        return StringUtils.camelCaseLower(this.toString());
    }


}
