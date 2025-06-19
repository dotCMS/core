package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotcms.util.JsonUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsEventsResultSerializer extends JsonSerializer<AnalyticsEventsResult>  {
    @Override
    public void serialize(final AnalyticsEventsResult analyticsEventsResult,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {

        final Map<String, Object> json = new HashMap<>();

        if (analyticsEventsResult.getFailed() != -1) {
            json.put("failed", analyticsEventsResult.getFailed());
        }

        if (analyticsEventsResult.getSuccess() != -1) {
            json.put("success", analyticsEventsResult.getSuccess());
        }

        json.put("status", analyticsEventsResult.getStatus());

        json.put("errors",
                analyticsEventsResult.getErrors().stream()
                        .map(AnalyticsEventsResultSerializer::getMap));

        jsonGenerator.writeObject(json);
    }

    private static Map<String, Object> getMap(AnalyticsValidatorUtil.Error error)  {
        try {
            final String errorJson = JsonUtil.getJsonStringFromObject(error);
            final Map<String, Object> map = JsonUtil.getJsonFromString(errorJson);

            if (error.getEventIndex() == -1) {
                map.remove("eventIndex");
            }

            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
