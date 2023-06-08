package com.dotcms.experiments.model;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class GoalsDeserializer extends JsonDeserializer<Goals> {

    private static ObjectMapper objectMapper = DotObjectMapperProvider.createDefaultMapper();

    @Override
    public Goals deserialize(JsonParser jsonParser, DeserializationContext ctxt)
            throws IOException {

        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final Metric primary = objectMapper.readValue(node.get("primary").toString(), Metric.class);
        final Goal goal = GoalFactory.create(primary);
        return Goals.builder().primary(goal).build();
    }
}
