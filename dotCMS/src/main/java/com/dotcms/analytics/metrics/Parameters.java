package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter.Type;

public abstract class Parameters {
    static Parameter URL = Parameter.builder()
            .name("url")
            .valueGetter(new EventAttributeParameterValuesGetter())
            .type(Type.CASE_INSENSITIVE)
            .build();

    static Parameter REFERER = Parameter.builder()
            .name("referer")
            .valueGetter(new EventAttributeParameterValuesGetter())
            .type(Type.CASE_INSENSITIVE)
            .build();

    static Parameter VISIT_BEFORE = Parameter.builder().name("visitBefore").validate(false).build();
}
