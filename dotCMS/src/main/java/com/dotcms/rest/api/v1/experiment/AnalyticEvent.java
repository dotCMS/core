package com.dotcms.rest.api.v1.experiment;

import java.util.List;
import java.util.Map;

public class AnalyticEvent {

    private List<Parameter> parameters;
    private AnalyticEventType eventKey;

    public AnalyticEvent(final AnalyticEventType eventKey, final List<Parameter> parameters) {
        this.parameters = parameters;
        this.eventKey = eventKey;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public AnalyticEventType getEventKey() {
        return eventKey;
    }

    public static class Parameter {
        final ParameterType parameterType;
        final String value;
        final Operator operator;

        public Parameter(final ParameterType parameterType,
                final String value,
                final Operator operator) {
            this.parameterType = parameterType;
            this.value = value;
            this.operator = operator;
        }

        public ParameterType getParameterType() {
            return parameterType;
        }

        public String getValue() {
            return value;
        }

        public Operator getOperator() {
            return operator;
        }
    }

    public enum Operator {
        CONTAINS,
        CONTAINS_ALL,
        START_WITH,
        END_WITH,
        IS;
    }

    public enum ParameterType {
        ID,
        CLASS,
        ELEMENT_TYPE,
        TARGET;
    }
}
