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
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * This conditionlet will allow CMS users to check the number of pages that a
 * user has visited during its current session. The information on the visited
 * pages will be available until the user's session ends.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-11-2015
 *
 */
public class UsersPageVisitsConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "number-visited-pages";
	private static final String CONDITIONLET_NAME = "User's Visited Pages";
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
			// Validate all available input fields
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
			// Set field configuration
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
		boolean result = false;
		if (UtilMethods.isSet(values) && values.size() > 0
				&& UtilMethods.isSet(comparisonId)) {
			Comparison comparison = getComparisonById(comparisonId);
			Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
			String inputValue = null;
			for (ConditionValue value : values) {
				inputValues.add(new ConditionletInputValue(INPUT_ID, value
						.getValue()));
				inputValue = value.getValue();
			}
			ValidationResults validationResults = validate(comparison,
					inputValues);
			if (!validationResults.hasErrors()) {
				int visitedPages = getTotalVisitedPages(request);
				int conditionletInput = Integer.parseInt(inputValue);
				if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
					if (visitedPages > conditionletInput) {
						return true;
					}
				} else if (comparison.getId().startsWith(
						COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
					if (visitedPages >= conditionletInput) {
						return true;
					}
				} else if (comparison.getId().startsWith(COMPARISON_EQUAL_TO)) {
					if (visitedPages == conditionletInput) {
						return true;
					}
				} else if (comparison.getId().endsWith(COMPARISON_LOWER_THAN)) {
					if (visitedPages < conditionletInput) {
						return true;
					}
				} else if (comparison.getId().endsWith(
						COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
					if (visitedPages <= conditionletInput) {
						return true;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Retrieves the number of pages that the user has visited in its current
	 * session under a specific site (host).
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The number of visited pages.
	 */
	private int getTotalVisitedPages(HttpServletRequest request) {
		String hostId = getHostId(request);
		if (UtilMethods.isSet(hostId)) {
			Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) request
					.getSession().getAttribute(
							WebKeys.RULES_CONDITIONLET_VISITEDURLS);
			if (visitedUrls != null && visitedUrls.containsKey(hostId)) {
				Set<String> urlSet = visitedUrls.get(hostId);
				if (urlSet != null) {
					return urlSet.size();
				}
			}
		}
		return 0;
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
		String hostId = null;
		try {
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			hostId = host.getIdentifier();
		} catch (PortalException | SystemException | DotDataException
				| DotSecurityException e) {
			Logger.error(this,
					"Could not retrieve current host information for: "
							+ request.getRequestURL());
		}
		return hostId;
	}

}
