package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotcms.util.JsonUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom JSON serializer for {@link AnalyticsEventsResult} objects.
 * This serializer converts AnalyticsEventsResult instances into a JSON representation
 * with fields for status, success count, failed count, and error details.
 * It handles special cases like omitting fields with -1 values and properly formatting error objects.
 */
public class AnalyticsEventsResultSerializer extends JsonSerializer<AnalyticsEventsResult>  {
    /**
     * Serializes an AnalyticsEventsResult object into JSON format.
     * 
     * @param analyticsEventsResult The AnalyticsEventsResult object to serialize
     * @param jsonGenerator The JSON generator used for writing JSON content
     * @param serializerProvider The serializer provider that can be used to get serializers for
     *                          serializing Objects value contains, if any
     * @throws IOException If an I/O error occurs during JSON generation
     */
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

    /**
     * Converts an AnalyticsValidatorUtil.Error object to a Map representation.
     * This method handles the conversion of error objects to a format suitable for JSON serialization.
     * If the eventIndex is -1, it removes this field from the resulting map.
     *
     * @param error The error object to convert
     * @return A Map containing the error information
     * @throws RuntimeException If an error occurs during the conversion process
     */
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
