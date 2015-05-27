package com.dotmarketing.portlets.rules.conditionlet;

import java.math.BigDecimal;
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

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * This conditionlet will allow CMS users to check the number of times a
 * specific user has visited a site.
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
	private static final String COMPARISON_LOWER_THAN = "lower";
	private static final String COMPARISON_LOWER_THAN_OR_EQUAL_TO = "lowerOrEqual";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	@Override
	protected String getName() {
		return CONDITIONLET_NAME;
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
			this.comparisons.add(new Comparison(COMPARISON_LOWER_THAN,
					"Is Lower Than"));
			this.comparisons.add(new Comparison(
					COMPARISON_LOWER_THAN_OR_EQUAL_TO,
					"Is Lower Than or Equal To"));
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
						+ INPUT_ID + "': '" + selectedValue + "'");
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
		String inputValue = null;
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(value.getId(), value
					.getValue()));
			inputValue = value.getValue();
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		String hostId = getHostId(request);
		if (!UtilMethods.isSet(hostId)) {
			return false;
		}
		Clickstream clickstream = (Clickstream) request
				.getSession().getAttribute("clickstream");
		int visitCounter = Integer.parseInt(inputValue);
		int visits = getSiteVisits(hostId);
		// Increase the visit counter cache with new sessions
		CacheLocator.getSiteVisitCache().addSiteVisit(hostId);
		// Take current session into account
		visits++;
		if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
			if (visits > visitCounter) {
				return true;
			}
		} else if (comparison.getId().startsWith(
				COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
			if (visits >= visitCounter) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_EQUAL_TO)) {
			if (visits == visitCounter) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_LOWER_THAN)) {
			if (visits < visitCounter) {
				return true;
			}
		} else if (comparison.getId().endsWith(
				COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
			if (visits <= visitCounter) {
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
	 * Returns the amount of visits to a specific site. The information
	 * 
	 * @param hostId
	 *            - The ID of the host that the URL belongs to.
	 * @return A {@code List<String>} containing the URLs that the specified
	 *         user has requested to the specified host.
	 */
	private int getSiteVisits(String hostId) {
		int visits = CacheLocator.getSiteVisitCache().getSiteVisits(hostId);
		if (visits < 0) {
			visits = 0;
			DotConnect dc = new DotConnect();
			String query = "select coalesce(sum(visits), 0) as visits "
					+ "from analytic_summary where host_id = ?";
			dc.setSQL(query);
			dc.addParam(hostId);
			try {
				List<Map<String, Object>> results = dc.loadObjectResults();
				for (Map<String, Object> record : results) {
					Object obj = record.get("visits");
					if (UtilMethods.isSet(obj)) {
						BigDecimal value = (BigDecimal) obj;
						visits = value.intValue();
					}
				}
				if (visits > 0) {
					CacheLocator.getSiteVisitCache().setSiteVisits(hostId,
							visits);
				}
			} catch (DotDataException e) {
				Logger.error(this,
						"An error occurred when executing the query.");
			}
		}
		return visits;
	}

}
