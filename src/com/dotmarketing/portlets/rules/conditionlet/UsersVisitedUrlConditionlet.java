package com.dotmarketing.portlets.rules.conditionlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ClickstreamRequest;
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
 * This conditionlet will allow CMS users to check whether a user has already
 * visited the current URL or not.
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
			if (Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", true)) {
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
		}
		return result;
	}

	/**
	 * Analyzes the URL in the HTTP request and determines whether it has been
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
	 *            - The URL or regular expression
	 * @param comparison
	 *            - The {@link Comparison} mechanism.
	 * @return If the URL meets the expected comparison criterion, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean isUrlVisited(HttpServletRequest request, String inputValue,
			Comparison comparison) {
		boolean isVisited = false;
		Clickstream clickstream = (Clickstream) request.getSession()
				.getAttribute("clickstream");
		if (clickstream != null && UtilMethods.isSet(inputValue)) {
			// Check Clickstream session object first
			List<String> sessionUrlList = getUrlsInSession(clickstream);
			isVisited = validateUrl(sessionUrlList, inputValue, comparison);
			// If not found, search previous database records
			if (!isVisited) {
				String hostId = getHostId(request);
				if (UtilMethods.isSet(hostId)) {
					List<String> dbUrlList = loadPreviousRecords(
							clickstream.getRemoteAddress(), hostId);
					dbUrlList.removeAll(sessionUrlList);
					isVisited = validateUrl(dbUrlList, inputValue, comparison);
				}
			}
		}
		return isVisited;
	}

	/**
	 * Retrieves the list of unique URLs that have been visited by the current
	 * session.
	 * 
	 * @param clickstream
	 *            - The {@link Clickstream} object for the current session.
	 * @return A {@code List<String>} containing the URLs visited by the current
	 *         session.
	 */
	private List<String> getUrlsInSession(Clickstream clickstream) {
		List<String> urlList = new ArrayList<String>();
		List<ClickstreamRequest> clickstreamRequests = clickstream
				.getClickstreamRequests();
		for (ClickstreamRequest click : clickstreamRequests) {
			String urlInRequest = click.getRequestURI();
			if (!urlList.contains(urlInRequest)) {
				urlList.add(urlInRequest);
			}
		}
		return urlList;
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
	private boolean validateUrl(List<String> urlList, String inputValue,
			Comparison comparison) {
		for (String urlInRequest : urlList) {
			if (comparison.getId().equals(COMPARISON_IS)) {
				if (inputValue.equalsIgnoreCase(urlInRequest)) {
					return true;
				}
			} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
				if (!inputValue.equalsIgnoreCase(urlInRequest)) {
					return true;
				}
			} else if (comparison.getId().startsWith(COMPARISON_STARTSWITH)) {
				if (inputValue.startsWith(urlInRequest)) {
					return true;
				}
			} else if (comparison.getId().endsWith(COMPARISON_ENDSWITH)) {
				if (inputValue.endsWith(urlInRequest)) {
					return true;
				}
			} else if (comparison.getId().endsWith(COMPARISON_CONTAINS)) {
				if (inputValue.contains(urlInRequest)) {
					return true;
				}
			} else if (comparison.getId().endsWith(COMPARISON_REGEX)) {
				Pattern pattern = Pattern.compile(inputValue);
				Matcher matcher = pattern.matcher(urlInRequest);
				if (matcher.find()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * If the current URL is not found in the list of visited URLs carried in
	 * the current session, this method will look it up in the database and will
	 * place the results in cache.
	 * 
	 * @param ipAddress
	 *            - The IP address of the client requesting the URL.
	 * @param hostId
	 *            - The ID of the host that the URL belongs to.
	 * @return A {@code List<String>} containing the URLs that the specified
	 *         user has requested to the specified host.
	 */
	private List<String> loadPreviousRecords(String ipAddress, String hostId) {
		List<String> visitedUrls = CacheLocator.getVisitedUrlCache().getUrls(
				ipAddress, hostId);
		if (!UtilMethods.isSet(visitedUrls)) {
			DotConnect dc = new DotConnect();
			String query = "SELECT DISTINCT(request_uri) FROM "
					+ "clickstream_request as cr inner join clickstream as c "
					+ "on cr.clickstream_id = c.clickstream_id "
					+ "where c.remote_address = ? and c.host_id = ?";
			dc.setSQL(query);
			dc.addParam(ipAddress);
			dc.addParam(hostId);
			try {
				List<Map<String, Object>> results = dc.loadObjectResults();
				visitedUrls = new ArrayList<String>();
				for (Map<String, Object> record : results) {
					Object obj = record.get("request_uri");
					if (UtilMethods.isSet(obj)) {
						String url = (String) record.get("request_uri");
						visitedUrls.add(url);
					}
				}
				if (visitedUrls.size() > 0) {
					CacheLocator.getVisitedUrlCache().addUrls(ipAddress,
							hostId, visitedUrls);
				}
			} catch (DotDataException e) {
				Logger.error(this,
						"An error occurred when executing the query.");
			}
		}
		return visitedUrls;
	}

}
