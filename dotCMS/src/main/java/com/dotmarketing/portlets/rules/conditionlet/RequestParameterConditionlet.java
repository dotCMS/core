package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EXISTS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestParameterConditionlet extends Conditionlet<RequestParameterConditionlet.Instance>{
	
	private static final long serialVersionUID = 1L;

    public static final String PARAMETER_NAME_KEY = "request-parameter";
    public static final String PARAMETER_VALUE_KEY = "request-parameter-value";

    private static final ParameterDefinition<TextType> parameterKey = new ParameterDefinition<>(
        1, PARAMETER_NAME_KEY, new TextInput<>(new TextType()));

    private static final ParameterDefinition<TextType> parameterValue = new ParameterDefinition<>(
        2, PARAMETER_VALUE_KEY, new TextInput<>(new TextType())
    );

    public RequestParameterConditionlet() {
        super("api.ruleengine.system.conditionlet.RequestParameter",
              parameterKey,
              new ComparisonParameterDefinition(2, IS, IS_NOT, EXISTS, STARTS_WITH, ENDS_WITH, CONTAINS, REGEX),
              parameterValue);
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String parameterActualValue = request.getParameter(instance.parameterKey);
        boolean evalSuccess;
        if(instance.comparison == EXISTS) {
            evalSuccess = EXISTS.perform(parameterActualValue);
        }
        else {
            if(parameterActualValue == null) {
                // treat null and empty string the same, except for 'Exists' case.
                parameterActualValue = "";
            }
            if(instance.comparison != REGEX) {
                //noinspection unchecked
                evalSuccess = instance.comparison.perform(parameterActualValue.toLowerCase(), instance.parameterValue.toLowerCase());
            } else {
                evalSuccess = REGEX.perform(parameterActualValue, instance.parameterValue);
            }
        }
        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String parameterKey;
        public final String parameterValue;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 3,
                       "Request Parameter Condition requires parameters %s, %s and %s.", PARAMETER_NAME_KEY, PARAMETER_VALUE_KEY, COMPARISON_KEY);
            assert parameters != null;
            this.parameterKey = parameters.get(PARAMETER_NAME_KEY).getValue();
            this.parameterValue = parameters.get(PARAMETER_VALUE_KEY).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }
}
