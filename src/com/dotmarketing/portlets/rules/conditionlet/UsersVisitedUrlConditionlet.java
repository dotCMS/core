package com.dotmarketing.portlets.rules.conditionlet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.util.HttpRequestDataUtil;
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
 * This conditionlet will allow CMS users to check whether a user has already
 * visited the current URL or not. The information on the visited pages will be
 * available until the user's session ends.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-23-2015
 *
 */
public class UsersVisitedUrlConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "has-visited-url";
	private static final String CONDITIONLET_NAME = "User's Visited URL";
	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";
	private static final String COMPARISON_STARTSWITH = "startsWith";
	private static final String COMPARISON_ENDSWITH = "endsWith";
	private static final String COMPARISON_CONTAINS = "contains";
	private static final String COMPARISON_REGEX = "regex";

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
			this.comparisons.add(new Comparison(COMPARISON_IS, "Is"));
			this.comparisons.add(new Comparison(COMPARISON_ISNOT, "Is Not"));
			this.comparisons.add(new Comparison(COMPARISON_STARTSWITH,
					"Starts With"));
			this.comparisons.add(new Comparison(COMPARISON_ENDSWITH,
					"Ends With"));
			this.comparisons
					.add(new Comparison(COMPARISON_CONTAINS, "Contains"));
			this.comparisons.add(new Comparison(COMPARISON_REGEX,
					"Matches Regular Expression"));
		}
		return this.comparisons;
	}

	@Override
	public ValidationResults validate(Comparison comparison,
			Set<ConditionletInputValue> inputValues) {
		ValidationResults results = new ValidationResults();
		if (UtilMethods.isSet(inputValues)) {
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
			if (UtilMethods.isSet(selectedValue)) {
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
				result = isUrlVisited(request, inputValue, comparison);
			}
		}
		return result;
	}

	/**
	 * Retrieves the URL in the HTTP request and determines whether it has been
	 * visited or not. Depending on the comparison mechanism, it can be a URL,
	 * only a section of it, or a regular expression.
	 * <p>
	 * It's worth noting that some URLs in dotCMS reference an {@code /index}
	 * page under the hood. For example, {@code /about-us/our-team} is actually
	 * {@code /about-us/our-team/index}. Bear this in mind when setting the
	 * value to check in the conditionlet data.
	 * </p>
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param inputValue
	 *            - The URL or regular expression.
	 * @param comparison
	 *            - The {@link Comparison} mechanism.
	 * @return If the URL meets the expected comparison criterion, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean isUrlVisited(HttpServletRequest request, String inputValue,
			Comparison comparison) {
		boolean isVisited = false;
		String hostId = getHostId(request);
		Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) request
				.getSession().getAttribute(
						WebKeys.RULES_CONDITIONLET_VISITEDURLS);
		Set<String> urlSet = null;
		if (visitedUrls == null) {
			visitedUrls = new HashMap<String, Set<String>>();
			urlSet = new LinkedHashSet<String>();
		} else {
			urlSet = visitedUrls.get(hostId);
		}
		isVisited = validateUrl(urlSet, inputValue, comparison);
		try {
			String uri = HttpRequestDataUtil.getUri(request);
			if (UtilMethods.isSet(uri)) {
				urlSet.add(uri);
				visitedUrls.put(hostId, urlSet);
				request.getSession().setAttribute(
						WebKeys.RULES_CONDITIONLET_VISITEDURLS, visitedUrls);
			}
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Could not retrieved a valid URI from request: "
					+ request.getRequestURL());
		}
		return isVisited;
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
			Logger.error(this, "Could not retrieve current host information");
		}
		return hostId;
	}

	/**
	 * Traverses a {@code List<String>} of visited URLs and performs the
	 * comparison in order to determine whether the input value of this
	 * conditionlet matches the requested URL.
	 * 
	 * @param urlList
	 *            - The list of visited URLs.
	 * @param inputValue
	 *            - The verification value specified by the conditionlet.
	 * @param comparison
	 *            - The {@link Comparison} object.
	 * @return If the requested URL matches the conditionlet value according to
	 *         the comparison, returns {@code true}. Otherwise, returns
	 *         {@code false}.
	 */
	private boolean validateUrl(Collection<String> urlList, String inputValue,
			Comparison comparison) {
		if (urlList != null && UtilMethods.isSet(inputValue)) {
			for (String urlInSession : urlList) {
				if (comparison.getId().equals(COMPARISON_IS)) {
					if (inputValue.equalsIgnoreCase(urlInSession)) {
						return true;
					}
				} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
					if (!inputValue.equalsIgnoreCase(urlInSession)) {
						return true;
					}
				} else if (comparison.getId().startsWith(COMPARISON_STARTSWITH)) {
					if (inputValue.startsWith(urlInSession)) {
						return true;
					}
				} else if (comparison.getId().endsWith(COMPARISON_ENDSWITH)) {
					if (inputValue.endsWith(urlInSession)) {
						return true;
					}
				} else if (comparison.getId().endsWith(COMPARISON_CONTAINS)) {
					if (inputValue.contains(urlInSession)) {
						return true;
					}
				} else if (comparison.getId().endsWith(COMPARISON_REGEX)) {
					Pattern pattern = Pattern.compile(inputValue);
					Matcher matcher = pattern.matcher(urlInSession);
					if (matcher.find()) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
