package com.dotcms.metrics;

import com.dotcms.http.request.StringPayloadHttpRequest;

public interface MetricsSender {

    String TOKEN_QUERY_PARAM_NAME = "token";

    void sendMetrics(StringPayloadHttpRequest httpRequest);

}
