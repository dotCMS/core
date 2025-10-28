package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Map;

/**
 * Serializer for {@link Goals
 */
public class GoalsSerializer extends JsonSerializer<Goals> {

    private static ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();

    @Override
    public void serialize(final Goals value, final JsonGenerator gen, SerializerProvider serializers)
            throws IOException {

        final Metric metric = value.primary().getMetric();
        final Map<String, Metric> goalsMap = Map.of("primary", metric);
        objectMapper.writeValue(gen, goalsMap);
    }
}
