package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;

public abstract class Conditionlet<T extends RuleComponentInstance> extends RuleComponentDefinition<T> {

    public static final String COMPARISON_KEY = "comparison";

    protected Conditionlet(String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(i18nKey, parameterDefinitions);
    }
}
