package com.dotmarketing.portlets.rules.conditionlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Config;
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
public class UsersSiteVisitsConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "site-visits";
	private static final String CONDITIONLET_NAME = "User's Visits to Site";

	private static final String COMPARISON_GREATER_THAN = "greater";
	private static final String COMPARISON_GREATER_THAN_OR_EQUAL_TO = "greaterOrEqual";
	private static final String COMPARISON_EQUAL_TO = "equal";
	private static final String COMPARISON_LOWER_THAN_OR_EQUAL_TO = "lowerOrEqual";
	private static final String COMPARISON_LOWER_THAN = "lower";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	public UsersSiteVisitsConditionlet() {
		super(CONDITIONLET_NAME);
	}

	@Override
	public Set<Comparison> getComparisons() {
		if (this.comparisons == null) {
			this.comparisons = new LinkedHashSet<Comparison>();
			this.comparisons.add(new Comparison(COMPARISON_GREATER_THAN,
					"Is Greater Than"));
			this.comparisons.add(new Comparison(
					COMPARISON_GREATER_THAN_OR_EQUAL_TO,
					"Is Greater Than or Equal To"));
			this.comparisons.add(new Comparison(COMPARISON_EQUAL_TO,
					"Is Equal To"));
			this.comparisons.add(new Comparison(
					COMPARISON_LOWER_THAN_OR_EQUAL_TO,
					"Is Lower Than or Equal To"));
			this.comparisons.add(new Comparison(COMPARISON_LOWER_THAN,
					"Is Lower Than"));
		}
		return this.comparisons;
	}

	@Override
	public ValidationResults validate(Comparison comparison,
			Set<ConditionletInputValue> inputValues) {
		ValidationResults results = new ValidationResults();
		if (UtilMethods.isSet(inputValues) && comparison != null) {
			List<ValidationResult> resultList = new ArrayList<ValidationResult>();
			for (ConditionletInputValue inputValue : inputValues) {
				ValidationResult validation = validate(comparison, inputValue);
				if (!validation.isValid()) {
					resultList.add(validation);
					results.setErrors(true);
				}
			}
			results.setResults(resultList);
		}
		return results;
	}

	@Override
	protected ValidationResult validate(Comparison comparison,
			ConditionletInputValue inputValue) {
		ValidationResult validationResult = new ValidationResult();
		String inputId = inputValue.getConditionletInputId();
		if (UtilMethods.isSet(inputId)) {
			String selectedValue = inputValue.getValue();
			if (Pattern.matches("\\d+", selectedValue)) {
				validationResult.setValid(true);
			} else {
				validationResult.setErrorMessage("Invalid value for input '"
						+ inputId + "': '" + selectedValue + "'");
			}
		}
		return validationResult;
	}

	@Override
	public Collection<ConditionletInput> getInputs(String comparisonId) {
		if (this.inputValues == null) {
			ConditionletInput inputField = new ConditionletInput();
			inputField.setId(INPUT_ID);
			inputField.setUserInputAllowed(true);
			inputField.setMultipleSelectionAllowed(false);
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			this.inputValues.put(inputField.getId(), inputField);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		if (!UtilMethods.isSet(values) || values.size() == 0
				|| !UtilMethods.isSet(comparisonId)) {
			return false;
		}
		if (!Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		String inputValue = values.get(0).getValue();
		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		String hostId = getHostId(request);
		String userId = getLoggedInUserId(request);
		if (!UtilMethods.isSet(hostId) || !UtilMethods.isSet(userId)) {
			return false;
		}
		int conditionletValue = Integer.parseInt(inputValue);
		int visits = getSiteVisits(userId, hostId);
		if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
			if (visits > conditionletValue) {
				return true;
			}
		} else if (comparison.getId().equals(
				COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
			if (visits >= conditionletValue) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_EQUAL_TO)) {
			if (visits == conditionletValue) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
			if (visits <= conditionletValue) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN)) {
			if (visits < conditionletValue) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the ID of the site (host) based on the {@code HttpServletRequest}
	 * object.
	 * 
	 * @param request
	 *            - The {@code HttpServletRequest} object.
	 * @return The ID of the site, or {@code null} if an error occurred when
	 *         retrieving the site information.
	 */
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

	/**
	 * Returns the ID of the logged-in user based on the
	 * {@code HttpServletRequest} object.
	 * 
	 * @param request
	 *            - The {@code HttpServletRequest} object.
	 * @return The ID of the user, or {@code null} if an error occurred when
	 *         retrieving the user information.
	 */
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

	/**
	 * Returns the number of times a user has visited the site that the URL
	 * belongs to.
	 * 
	 * @param userId
	 *            - The ID of the currently logged-in user.
	 * @param hostId
	 *            - The ID of the host that the URL belongs to.
	 * 
	 * @return The number of visits.
	 */
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

}
