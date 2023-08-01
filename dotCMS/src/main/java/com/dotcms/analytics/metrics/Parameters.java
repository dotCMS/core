package com.dotcms.analytics.metrics;

public abstract class Parameters {
    static Parameter URL = Parameter.builder()
            .name("url")
            .valueGetter(new LowerCaseParameterValuesGetter(new EventAttributeParameterValuesGetter()))
            .build();

    static Parameter VISIT_BEFORE = Parameter.builder().name("visitBefore").validate(false).build();
}
