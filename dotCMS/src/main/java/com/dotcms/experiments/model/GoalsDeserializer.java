package com.dotcms.experiments.model;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.JsonUtil;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class GoalsDeserializer extends JsonDeserializer<Goals> {

    private static ObjectMapper objectMapper = DotObjectMapperProvider.createDefaultMapper();

    @Override
    public Goals deserialize(JsonParser jsonParser, DeserializationContext ctxt)
            throws IOException {

        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        final Map goalAsMap = objectMapper.readValue(node.get("primary").toString(), Map.class);

        transformConditionValuesToString(goalAsMap);

        final String goalJsonString = JsonUtil.getJsonAsString(goalAsMap);
        final Metric primary = objectMapper.readValue(goalJsonString, Metric.class);
        final Goal goal = GoalFactory.create(primary);
        return Goals.builder().primary(goal).build();
    }

    /**
     * Transform all the Condition's values than are set as Map to String Json values
     * @param goalAsMap
     * @throws IOException
     */
    private static void transformConditionValuesToString(Map goalAsMap) throws IOException {
        final Collection<Map> conditionsAsMap = (Collection) goalAsMap.get("conditions");

        for (final Map condition : conditionsAsMap) {
            final Object value = condition.get("value");

            if (value instanceof Map) {
                final String valueAsJsonString = JsonUtil.getJsonAsString((Map) value);
                condition.put("value", valueAsJsonString);
            }
        }
    }
}
