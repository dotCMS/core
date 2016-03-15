package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.util.NumberOfTimeVisitedCounter;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.util.WebKeys;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN_OR_EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN_OR_EQUAL;


public class UsersSiteVisitsConditionlet extends Conditionlet<UsersSiteVisitsConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	public static final String SITE_VISITS_KEY = "site-visits";
	
	private static final ParameterDefinition<NumericType> siteVisitsValue =
			new ParameterDefinition<>(1,SITE_VISITS_KEY, new NumericInput<>(new NumericType().minValue(0)));
	
	public UsersSiteVisitsConditionlet() {
        super("api.ruleengine.system.conditionlet.SiteVisits",
              new ComparisonParameterDefinition(2, EQUAL, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL),
              siteVisitsValue);
    }


	@Override
	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {

		String siteVisits = String.valueOf( NumberOfTimeVisitedCounter.getNumberSiteVisits( request ) );
		String siteVisitsValue = instance.siteVisits;

		return instance.comparison.perform(siteVisits, siteVisitsValue);
		
	}

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {
    	
    	private final String siteVisits;
    	private final Comparison<String> comparison;
    	
    	private Instance(UsersSiteVisitsConditionlet definition, Map<String, ParameterModel> parameters){
    		this.siteVisits = parameters.get(SITE_VISITS_KEY).getValue();
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