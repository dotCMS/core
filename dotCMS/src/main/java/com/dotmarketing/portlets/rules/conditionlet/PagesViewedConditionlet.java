package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import java.security.InvalidParameterException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.dotmarketing.factories.ClickstreamFactory.CLICKSTREAM_SESSION_ATTR_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.NUMERIC_COMPARISONS;

/**
 * Counts the total Page hits, including repeat Page hits, for the visitors current session.
 */
public class PagesViewedConditionlet extends Conditionlet<PagesViewedConditionlet.Instance> {

    public static final String NUMBER_PAGES_VIEWED_INPUT_KEY = "number-pages-viewed-input-key";
    public static final String PAGES_VIEWED_KEY = "api.system.ruleengine.conditionlet.PagesViewed";

    private static final ParameterDefinition<NumericType> NUMBER_PAGES_VIEWED = new ParameterDefinition<>(3, NUMBER_PAGES_VIEWED_INPUT_KEY,
            new NumericInput<>(new NumericType().required().minValue(0)));

    public PagesViewedConditionlet() {
        super(PAGES_VIEWED_KEY, new ComparisonParameterDefinition(2, NUMERIC_COMPARISONS), NUMBER_PAGES_VIEWED);
    }

	@Override
	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {

        HttpSession session = request.getSession(true);

        Clickstream clickstream = (Clickstream)session.getAttribute(CLICKSTREAM_SESSION_ATTR_KEY);
        if(clickstream == null) {
            clickstream = new Clickstream();
            session.setAttribute(CLICKSTREAM_SESSION_ATTR_KEY, clickstream);
		}

        int actualCount = clickstream.getNumberOfRequests();
        return instance.comparison.perform(actualCount, instance.totalPageViewsCount);
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        private final int totalPageViewsCount;
        private final Comparison<Number> comparison;

        private Instance(PagesViewedConditionlet definition, Map<String, ParameterModel> parameters) {
            try {
                this.totalPageViewsCount = Integer.parseInt(parameters.get(NUMBER_PAGES_VIEWED_INPUT_KEY).getValue());
            } catch (NumberFormatException e) {
                throw new InvalidParameterException(NUMBER_PAGES_VIEWED_INPUT_KEY + " must be an integer value");
            }
    		String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
    		try {
				//noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition)definition
                    .getParameterDefinitions()
                    .get(COMPARISON_KEY))
                    .comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue, definition.getId());
            }
        }
    }

    }