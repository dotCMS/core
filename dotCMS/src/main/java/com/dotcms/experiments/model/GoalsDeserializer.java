package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.analytics.metrics.Parameter;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class GoalsDeserializer extends JsonDeserializer<Goals> {

    private static ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getObjectMapper();

    @Override
    public Goals deserialize(JsonParser jsonParser, DeserializationContext ctxt)
            throws IOException {

        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        final Map goalAsMap = objectMapper.readValue(node.get("primary").toString(), Map.class);

        final Collection<Condition<Object>> conditions = getConditions(goalAsMap);
        final String goalJsonString = JsonUtil.getJsonAsString(goalAsMap);
        final Metric primary = objectMapper.readValue(goalJsonString, Metric.class);

        final Goal goal = GoalFactory.create(primary.withConditions(conditions));
        return Goals.builder().primary(goal).build();
    }

    /**
     * Transform all the Condition's values than are set as Map to String Json values
     * @param goalAsMap
     * @throws IOException
     */
    private static Collection<Condition<Object>> getConditions(final Map goalAsMap) throws IOException {
        final Collection<Condition<Object>> conditions = new ArrayList<>();
        final MetricType metricType = MetricType.valueOf(goalAsMap.get("type").toString());
        final Collection<Map> conditionsAsMap = (Collection) goalAsMap.get("conditions");

        if (UtilMethods.isSet(conditionsAsMap)) {
            for (final Map condition : conditionsAsMap) {
                final Object value = condition.get("value");
                final String parameterName = condition.get("parameter").toString();
                final Parameter parameter = metricType.getParameter(parameterName)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Parameters provided: " + parameterName));

                final String valueAsJsonString = value instanceof Map ? JsonUtil.getJsonAsString((Map) value) :
                        value.toString();

                final Object deserialize = parameter.type().getTransformer()
                        .deserialize(valueAsJsonString);

                conditions.add(
                    Condition.builder()
                            .operator(Operator.valueOf(condition.get("operator").toString()))
                            .parameter(parameterName)
                            .value(deserialize)
                            .build()
                );
            }
        }

        return conditions;
    }
}
