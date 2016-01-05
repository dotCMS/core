package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.comparison.EqualComparison;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;


/**
 * This conditionlet will allow CMS users to check the number of times that <b>A
 * LOGGED-IN USER</b> has visited a site. This {@link Conditionlet} provides a
 * drop-down menu with the available comparison mechanisms, and a text field to
 * enter the number of visits to compare.
 * <p>
 * This conditionlet uses Clickstream, which is a user tracking component for
 * Java web applications, and it must be enabled in your dotCMS configuration.
 * To do it, just go to the {@code dotmarketing-config.properties} file, look
 * for a property called {@code ENABLE_CLICKSTREAM_TRACKING} and set its value
 * to {@code true}. You can also take a look at the other Clickstream
 * configuration properties to have it running according to your environment's
 * resources.
 * </p>
 * <p>
 * The information on the number of times as user visits a specific site is
 * stored in the database. A new entry will be added every time the session of
 * an authenticated user ends. The Dashboard job in dotCMS will be in charge of,
 * among other tasks, generating the report on the number of times a specific
 * user has visited a site. The {@code dotmarketing-config.properties} file
 * contains a property called {@code DASHBOARD_POPULATE_TABLES_CRON_EXPRESSION},
 * which determines the moment that the job is scheduled to run. You can change
 * this property accordingly.
 * </p>
 *
 * @author Jose Castro
 * @version 1.0
 * @since 05-04-2015
 *
 */
public class UsersSiteVisitsConditionlet extends Conditionlet<UsersSiteVisitsConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	private static final String SITE_VISITS_KEY = "site-visits";
	
	private static final ParameterDefinition<NumericType> siteVisitsValue = new ParameterDefinition<>(1,SITE_VISITS_KEY,new NumericInput<>(new NumericType()));
	
	public UsersSiteVisitsConditionlet() {
        super("api.ruleengine.system.conditionlet.SiteVisits",
              new ComparisonParameterDefinition(2, EQUAL, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL),
              siteVisitsValue);
    }


	@Override
	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
		return false;
	}

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {
    	
    	private final String siteVisits;
    	private final Comparison comparison;
    	
    	private Instance(UsersSiteVisitsConditionlet definition, Map<String, ParameterModel> parameters){
    		this.siteVisits = parameters.get(SITE_VISITS_KEY).getValue();
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
