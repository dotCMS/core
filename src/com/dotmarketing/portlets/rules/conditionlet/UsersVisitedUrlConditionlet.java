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
import java.util.regex.PatternSyntaxException;

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
 * available until the user's session ends. The comparison of URLs is
 * case-insensitive, except for the regular expression comparison. This
 * {@link Conditionlet} provides a drop-down menu with the available comparison
 * mechanisms, and a text field to enter the value to compare. The user session
 * has a {@link Map} object holding the URLs that the user has visited per site.
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

    public UsersVisitedUrlConditionlet() {
        super(CONDITIONLET_NAME);
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
			String comparisonId = comparison.getId();
			if (comparisonId.equals(COMPARISON_IS)
					|| comparisonId.equals(COMPARISON_ISNOT)
					|| comparisonId.equals(COMPARISON_STARTSWITH)
					|| comparisonId.equals(COMPARISON_ENDSWITH)
					|| comparisonId.equals(COMPARISON_CONTAINS)) {
				if (UtilMethods.isSet(selectedValue)) {
					validationResult.setValid(true);
				}
			} else if (comparisonId.equals(COMPARISON_REGEX)) {
				try {
					Pattern.compile(selectedValue);
					validationResult.setValid(true);
				} catch (PatternSyntaxException e) {
					Logger.debug(this, "Invalid RegEx " + selectedValue);
				}
			}
			if (!validationResult.isValid()) {
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
			inputField.setMinNum(1);
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
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		String inputValue = values.get(0).getValue();
		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		return checkVisitedUrls(request, inputValue, comparison);
	}

	/**
	 * Retrieves the URL from the HTTP request and determines whether it has
	 * been visited or not. Depending on the comparison mechanism, it can be a
	 * URL, only a section of it, or a regular expression.
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
	private boolean checkVisitedUrls(HttpServletRequest request,
			String inputValue, Comparison comparison) {
		String hostId = getHostId(request);
		if (!UtilMethods.isSet(hostId)) {
			return false;
		}
		Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) request
				.getSession(true).getAttribute(
						WebKeys.RULES_CONDITIONLET_VISITEDURLS);
		Set<String> urlSet = null;
		if (visitedUrls == null) {
			visitedUrls = new HashMap<String, Set<String>>();
			urlSet = new LinkedHashSet<String>();
		} else {
			urlSet = visitedUrls.get(hostId);
		}
		boolean checkedUrl = validateUrl(urlSet, inputValue, comparison);
		try {
			String uri = HttpRequestDataUtil.getUri(request);
			if (UtilMethods.isSet(uri) && !urlSet.contains(uri)) {
				urlSet.add(uri);
				visitedUrls.put(hostId, urlSet);
				request.getSession(true).setAttribute(
						WebKeys.RULES_CONDITIONLET_VISITEDURLS, visitedUrls);
			}
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Could not retrieved a valid URI from request: "
					+ request.getRequestURL());
		}
		return checkedUrl;
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
		if (comparison.getId().equals(COMPARISON_IS)) {
			for (String urlInSession : urlList) {
				if (urlInSession.equalsIgnoreCase(inputValue)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
			boolean found = false;
			for (String urlInSession : urlList) {
				if (urlInSession.equalsIgnoreCase(inputValue)) {
					found = true;
					break;
				}
			}
			return !found;
		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
			for (String urlInSession : urlList) {
				if (urlInSession.startsWith(inputValue)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
			for (String urlInSession : urlList) {
				if (urlInSession.endsWith(inputValue)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
			for (String urlInSession : urlList) {
				if (urlInSession.contains(inputValue)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
			for (String urlInSession : urlList) {
				Pattern pattern = Pattern.compile(inputValue);
				Matcher matcher = pattern.matcher(urlInSession);
				if (matcher.find()) {
					return true;
				}
			}
		}
		return false;
	}

}
