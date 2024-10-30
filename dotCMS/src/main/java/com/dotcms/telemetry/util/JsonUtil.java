package com.dotcms.telemetry.util;

import com.dotcms.telemetry.business.MetricEndpointPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Util class to handle Json format
 */
public enum JsonUtil {

    INSTANCE;

    static final JsonMapper jsonMapper = new JsonMapper();

    public String getAsJson(final MetricEndpointPayload metricEndpointPayload)  {
        try {
            return jsonMapper.writeValueAsString(metricEndpointPayload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
