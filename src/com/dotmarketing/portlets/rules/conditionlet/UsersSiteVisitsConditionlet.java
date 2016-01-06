package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN_OR_EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN_OR_EQUAL;
import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
	
	private static final ParameterDefinition<NumericType> siteVisitsValue = new ParameterDefinition<>(3,SITE_VISITS_KEY,new NumericInput<>(new NumericType()));
	
	public UsersSiteVisitsConditionlet() {
        super("api.ruleengine.system.conditionlet.SiteVisits",
              new ComparisonParameterDefinition(2, EQUAL, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL),
              siteVisitsValue);
    }

//	/**
//	 * Returns the number of times a user has visited the site that the URL
//	 * belongs to.
//	 *
//	 * @param userId
//	 *            - The ID of the currently logged-in user.
//	 * @param hostId
//	 *            - The ID of the host that the URL belongs to.
//	 *
//	 * @return The number of visits.
//	 */
	public int getSiteVisits(String userId, String hostId) {
		int visits = CacheLocator.getSiteVisitCache().getSiteVisits(userId,
				hostId);
		if (visits < 0) {
			DotConnect dc = new DotConnect();
			String query = "SELECT visits FROM analytic_summary_user_visits WHERE user_id = ? AND host_id = ?";
			dc.setSQL(query);
			dc.addParam(userId);
			dc.addParam(hostId);
			try {
				List<Map<String, Object>> results = dc.loadObjectResults();
				if (!results.isEmpty()) {
					Map<String, Object> record = results.get(0);
					Long visitsLong = (Long) record.get("visits");
					visits = visitsLong.intValue();
				}
				if (visits > 0) {
					CacheLocator.getSiteVisitCache().setSiteVisits(userId,
							hostId, visits);
				}
			} catch (DotDataException e) {
				Logger.error(this,
						"An error occurred when executing the query.");
			}
		}
		return visits;
	}
	

//	/**
//	 * Returns the ID of the site (host) based on the {@code HttpServletRequest}
//	 * object.
//	 *
//	 * @param request
//	 *            - The {@code HttpServletRequest} object.
//	 * @return The ID of the site, or {@code null} if an error occurred when
//	 *         retrieving the site information.
//	 */
	private String getHostId(HttpServletRequest request) {
		try {
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			if (host != null) {
				return host.getIdentifier();
			}
		} catch (PortalException | SystemException | DotDataException
				| DotSecurityException e) {
			Logger.error(this, "Could not retrieve current host information.");
		}
		return null;
	}
	
//	/**
//	 * Returns the ID of the logged-in user based on the
//	 * {@code HttpServletRequest} object.
//	 *
//	 * @param request
//	 *            - The {@code HttpServletRequest} object.
//	 * @return The ID of the user, or {@code null} if an error occurred when
//	 *         retrieving the user information.
//	 */
	private String getLoggedInUserId(HttpServletRequest request) {
		try {
			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			if (user != null) {
				return user.getUserId();
			}
		} catch (DotRuntimeException | PortalException | SystemException e) {
			Logger.error(this, "Could not retrieve current user information.");
		}
		return null;
	}
	
	@Override
	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
		if (!Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
			return false;
		}
		String hostId = getHostId(request);
		String userId = getLoggedInUserId(request);
		int siteVisitsValue = Integer.parseInt(instance.siteVisits);
		if (!UtilMethods.isSet(hostId) || !UtilMethods.isSet(userId)) {
			return false;
		}
		int visits = getSiteVisits(userId, hostId);
        return instance.comparison.perform(visits,siteVisitsValue);
	}

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {
    	
    	private final String siteVisits;
    	private final Comparison<Integer> comparison;
    	
    	private Instance(UsersSiteVisitsConditionlet definition, Map<String, ParameterModel> parameters){
    		checkState(parameters != null && parameters.size() == 2, "Users Site Visits Condition requires parameters %s and %s.", COMPARISON_KEY, SITE_VISITS_KEY);
            assert parameters != null;
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
