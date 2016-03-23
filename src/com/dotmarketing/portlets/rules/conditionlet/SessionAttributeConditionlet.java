package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * This conditionlet will allow CMS users to check the value of any of the HTTP
 * Session that are part of the {@link HttpSession} object. The
 * comparison of session attibute and values is case-insensitive, except for the
 * regular expression comparison.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 03-10-2016
 */
public class SessionAttributeConditionlet extends Conditionlet<SessionAttributeConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "sessionKey";
    public static final String SESSION_VALUE = "sessionValue";

    private static final ParameterDefinition<TextType> sessionKey = new ParameterDefinition<>(
        1, SESSION_KEY,new TextInput<>(new TextType().required()));

    private static final ParameterDefinition<TextType> sessionValue = new ParameterDefinition<>(
        2, SESSION_VALUE, new TextInput<>(new TextType().required())
    );

    public SessionAttributeConditionlet() {
        super("api.system.ruleengine.conditionlet.SessionAttribute",
                sessionKey,
                new ComparisonParameterDefinition(2, IS, IS_NOT, EXISTS, STARTS_WITH, ENDS_WITH, CONTAINS, REGEX),
                sessionValue);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        boolean evalSuccess;

        if(request.getSession().getAttribute(instance.sessionKey) == null){
            evalSuccess = false;
        } else {
            String sessionActualValue = request.getSession().getAttribute(instance.sessionKey).toString();

            if(instance.comparison == EXISTS) {
                evalSuccess = EXISTS.perform(sessionActualValue);
            }
            else {
                if(instance.comparison != REGEX) {
                    //noinspection unchecked
                    evalSuccess = instance.comparison.perform(sessionActualValue.toLowerCase(), instance.sessionValue.toLowerCase());
                } else {
                    evalSuccess = REGEX.perform(sessionActualValue, instance.sessionValue);
                }
            }
        }

        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String sessionKey;
        public final String sessionValue;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 3,
                       "Session Attribute Condition requires parameters %s, %s and %s.", SESSION_KEY, SESSION_VALUE, COMPARISON_KEY);
            assert parameters != null;
            this.sessionKey = parameters.get(SESSION_KEY).getValue();
            this.sessionValue = parameters.get(SESSION_VALUE).getValue();
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
