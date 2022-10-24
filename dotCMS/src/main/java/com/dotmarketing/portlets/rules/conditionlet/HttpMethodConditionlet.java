package com.dotmarketing.portlets.rules.conditionlet;

import org.apache.logging.log4j.util.Strings;
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
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

/**
  * this conditionlet will allow a user to fire a rule based on the incominmg HTTP Method.
  *
  * @author Jose Castro
  * @version 1.0
  * @since 04-22-2015
 */

public class HttpMethodConditionlet extends Conditionlet<HttpMethodConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String HTTP_METHOD = "http-method";

    private static final ParameterDefinition<TextType> httpMethodParam = new ParameterDefinition<>(
            3, HTTP_METHOD,
            new DropdownInput(new TextType().maxLength(255))
            .allowAdditions()
            .maxSelections(10)
            .minSelections(1)
            .option("GET")
            .option("POST")
            .option("PUT")
            .option("PATCH")
            .option("DELETE")
            .option("OPTIONS")
            .option("HEAD")
    );

    public HttpMethodConditionlet() {
        super("api.ruleengine.system.conditionlet.HttpMethodConditionlet",
                new ComparisonParameterDefinition(2, IS, IS_NOT, STARTS_WITH, ENDS_WITH, CONTAINS, REGEX),
                httpMethodParam);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String httpMethodActualValue = request.getMethod();

        if(Strings.isBlank(httpMethodActualValue)) {
            return false;
        }

        boolean evalSuccess;

        if(instance.comparison != REGEX) {
            //noinspection unchecked
            evalSuccess = instance.comparison.perform(httpMethodActualValue.toLowerCase(), instance.httpMethodValue.toLowerCase());
        } else {
            evalSuccess = REGEX.perform(httpMethodActualValue,  instance.httpMethodValue);
        }
        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String httpMethodValue;
        public final Comparison<String> comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 2, "Http Method requires parameters %s and %s.", COMPARISON_KEY, HTTP_METHOD);
            assert parameters != null;
            this.httpMethodValue = parameters.get(HTTP_METHOD).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                //noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }
}
