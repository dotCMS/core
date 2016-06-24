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
import javax.servlet.http.HttpSession;

/**
 * This conditionlet will allow CMS users to check the value of any of the HTTP
 * Request that are part of the {@link HttpSession} object. The
 * comparison of request attibute and values is case-insensitive, except for the
 * regular expression comparison.
 *
 * @author Erick Gonzalez
 * @version 1.0
 * @since 03-10-2016
 */

public class RequestAttributeConditionlet extends Conditionlet<RequestAttributeConditionlet.Instance>{
	
	private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_NAME_KEY = "request-attribute";
    public static final String ATTRIBUTE_VALUE_KEY = "request-attribute-value";

    private static final ParameterDefinition<TextType> attributeKey = new ParameterDefinition<>(
        1, ATTRIBUTE_NAME_KEY, new TextInput<>(new TextType()));

    private static final ParameterDefinition<TextType> attributeValue = new ParameterDefinition<>(
        2, ATTRIBUTE_VALUE_KEY, new TextInput<>(new TextType())
    );

    public RequestAttributeConditionlet() {
        super("api.ruleengine.system.conditionlet.RequestAttribute",
              attributeKey,
              new ComparisonParameterDefinition(2, IS, IS_NOT, EXISTS, STARTS_WITH, ENDS_WITH, CONTAINS, REGEX),
              attributeValue);
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String attributeActualValue = request.getAttribute(instance.attributeKey).toString();
        boolean evalSuccess;
        if(instance.comparison == EXISTS) {
            evalSuccess = EXISTS.perform(attributeActualValue);
        }
        else {
            if(attributeActualValue == null) {
                // treat null and empty string the same, except for 'Exists' case.
                attributeActualValue = "";
            }
            if(instance.comparison != REGEX) {
                //noinspection unchecked
                evalSuccess = instance.comparison.perform(attributeActualValue.toLowerCase(), instance.attributeValue.toLowerCase());
            } else {
                evalSuccess = REGEX.perform(attributeActualValue, instance.attributeValue);
            }
        }
        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String attributeKey;
        public final String attributeValue;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 3,
                       "Request Attribute Condition requires parameters %s, %s and %s.", ATTRIBUTE_NAME_KEY, ATTRIBUTE_VALUE_KEY, COMPARISON_KEY);
            assert parameters != null;
            this.attributeKey = parameters.get(ATTRIBUTE_NAME_KEY).getValue();
            this.attributeValue = parameters.get(ATTRIBUTE_VALUE_KEY).getValue();
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
