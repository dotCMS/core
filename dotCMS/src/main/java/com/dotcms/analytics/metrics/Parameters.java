package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter.Type;

public final class Parameters {

    private Parameters(){}

    static Parameter URL = Parameter.builder()
            .name("url")
            .build();

    static Parameter QUERY_PARAMETER = Parameter.builder().name("queryParameter")
            .type(AbstractCondition.AbstractParameter.Type.QUERY_PARAMETER)
            .build();
}
