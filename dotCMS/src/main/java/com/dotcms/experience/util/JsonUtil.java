package com.dotcms.experience.util;

import com.dotcms.experience.business.MetricEndpointPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Util class to handle Json format
 */
public enum JsonUtil {

    INSTANCE;

    final static JsonMapper JSON_MAPPER = new JsonMapper();

    public String getAsJson(final MetricEndpointPayload metricEndpointPayload)  {
        try {
            return JSON_MAPPER.writeValueAsString(metricEndpointPayload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
