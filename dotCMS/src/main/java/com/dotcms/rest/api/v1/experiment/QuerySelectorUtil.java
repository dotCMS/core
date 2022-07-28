package com.dotcms.rest.api.v1.experiment;

import static com.dotcms.rest.api.v1.experiment.AnalyticEvent.Operator.*;
import static com.dotcms.rest.api.v1.experiment.AnalyticEvent.ParameterType.*;
import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.rest.api.v1.experiment.AnalyticEvent.Operator;
import com.dotcms.rest.api.v1.experiment.AnalyticEvent.Parameter;
import com.dotcms.rest.api.v1.experiment.AnalyticEvent.ParameterType;
import com.dotmarketing.util.UtilMethods;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum QuerySelectorUtil {

    INSTANCE;

    private class Builder {
         private final Map<ParameterType, Map<Operator, Function<String, List<String>>>> builders =
                 new HashMap<>();

         void put(ParameterType parameterType, Operator operator, Function<String, List<String>> handler) {
             Map<Operator, Function<String, List<String>>> operatorFunctionMap = builders.get(
                     parameterType);

             if (!UtilMethods.isSet(operatorFunctionMap)) {
                 operatorFunctionMap  = new LinkedHashMap<>();
                 builders.put(parameterType, operatorFunctionMap);
             }

             operatorFunctionMap.put(operator, handler);
         }

        public Optional<Function<String, List<String>>> get(
                final ParameterType parameterType,
                final Operator operator) {
            final Map<Operator, Function<String, List<String>>> operatorFunctionMap = builders.get(
                    parameterType);
            return Optional.ofNullable(UtilMethods.isSet(operatorFunctionMap) ? operatorFunctionMap.get(operator) : null);
        }
    }

    private Builder builder  = new Builder();

    QuerySelectorUtil() {
       builder.put(ID, IS, (String value) -> list(String.format("#%s", value)));

        builder.put(ELEMENT_TYPE, IS, (String value) -> list(value));
        builder.put(ELEMENT_TYPE, CONTAINS_ALL, (String value) -> Arrays.asList(value.split(",")));

        builder.put(CLASS, CONTAINS, (String value) -> list(String.format(".%s", value)));
        builder.put(CLASS, CONTAINS_ALL, (String value) -> Arrays.stream(value.split(","))
                .map(className -> String.format(".%s", value))
                .collect(Collectors.toList()));

        builder.put(TARGET, IS, (String value) -> list(String.format("[href=\"%s\"]", value)));
        builder.put(TARGET, START_WITH,
                (String value) -> list(String.format("[href^=\"%s\"]", value)));
        builder.put(TARGET, END_WITH,
                (String value) -> list(String.format("[href$=\"%s\"]", value)));
    }

    public String getQuerySelector(final List<Parameter> parameters) {
        List<String> querySelectorItems  = null;

        for (final Parameter parameter : parameters) {
            final Optional<Function<String, List<String>>> handler = builder.get(
                    parameter.getParameterType(), parameter.getOperator());

            if (handler.isPresent()) {
                final List<String> result = handler.get().apply(parameter.getValue());

                if (UtilMethods.isSet(querySelectorItems)) {
                    querySelectorItems = result;
                } else {
                    for (String newItemToAppend : result) {
                        querySelectorItems  = querySelectorItems.stream()
                                .map(oldItem -> oldItem  + newItemToAppend)
                                .collect(Collectors.toList());
                    }
                }
            }
        }

        return querySelectorItems.stream().collect(Collectors.joining(","));
    }
}
