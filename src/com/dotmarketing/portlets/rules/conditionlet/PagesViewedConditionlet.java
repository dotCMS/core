package com.dotmarketing.portlets.rules.conditionlet;


import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * This {@link Conditionlet} will allow CMS users to execute Actionlets based on the Number of page visited,
 * the users can insert one number to compare.
 *
 * @author Freddy Rodriguez
 *         Date: 26/01/16.

 */
public class PagesViewedConditionlet extends Conditionlet<PagesViewedConditionlet.Instance>{

    public static final String NUMBER_PAGES_VIEWED_INPUT_KEY = "number-pages-viewed-input-key";
    public static final String Pages_VIEWED_KEY = "api.ruleengine.system.conditionlet.PagesViewed";

    private static final ParameterDefinition<NumericType> NUMBER_PAGES_VIEWED = new ParameterDefinition<>(3, NUMBER_PAGES_VIEWED_INPUT_KEY,
            new NumericInput<>(new NumericType()));

    public PagesViewedConditionlet() {
        super(Pages_VIEWED_KEY, new ComparisonParameterDefinition(2, NUMERIC_COMPARATION), NUMBER_PAGES_VIEWED);
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        Visitor visitor = (Visitor) request.getSession().getAttribute(WebKeys.VISITOR);
        int nPagesViewed = visitor.getNumberPagesViewed();
        return instance.comparison.perform(nPagesViewed, instance.numberPagesViewedInput);
    }

    public static class Instance implements RuleComponentInstance {
        private final int numberPagesViewedInput;
        private final Comparison<Number> comparison;
        private final String comparisonValue;

        private Instance(PagesViewedConditionlet definition, Map<String, ParameterModel> parameters) {
            this.numberPagesViewedInput = Integer.parseInt( parameters.get(NUMBER_PAGES_VIEWED_INPUT_KEY).getValue() );
            this.comparisonValue = parameters.get(COMPARISON_KEY).getValue();

            try {
                // noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition) definition.getParameterDefinitions().get(
                        COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException(
                        "The comparison '%s' is not supported on Condition type '%s'", comparisonValue,
                        definition.getId());
            }
        }

    }
}
