package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Conditionlet<T extends RuleComponentInstance> extends RuleComponentDefinition<T> {

    public static final String COMPARISON_KEY = "comparison";

    /**
     * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    protected Conditionlet(String id, String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(id, i18nKey, parameterDefinitions);
    }

    protected Conditionlet(String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(i18nKey, parameterDefinitions);
    }

    /**
     * @param json A JSON-bindable data structure
     * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    @Deprecated
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Conditionlet fromJson(RuleComponentDefinition.Json json) {

        return new Conditionlet(json.id, json.i18nKey,
                json.parameterDefinitions.values().toArray(new ParameterDefinition[]{})) {

            @Override
            public RuleComponentInstance instanceFrom(Map parameters) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean evaluate(HttpServletRequest request, HttpServletResponse response,
                    RuleComponentInstance instance) {
                throw new UnsupportedOperationException();
            }
        };
    }

}