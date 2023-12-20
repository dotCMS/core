package com.dotcms.analytics.experience.metric;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

public enum MetricCategory {
    KEY_FEATURES(),
    PLATFORM_SPECIFIC_CUSTOMIZATION(),
    SOPHISTICATED_CONTENT_ARCHITECTURE(),
    POSITIVE_USER_EXPERIENCE(),
    PLATFORM_SPECIFIC_DEVELOPMENT(),
    RECENT_ACTIVITY(),
    EXTENT_OF_USAGE();

    @JsonValue
    public String getCamelCaseName()  {
        return StringUtils.camelCaseLower(this.toString());
    }

}
