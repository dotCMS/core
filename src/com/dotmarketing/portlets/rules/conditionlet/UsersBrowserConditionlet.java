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
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.eu.bitwalker.useragentutils.UserAgent;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the browser name a user
 * request is issued from. The information is obtained by reading the
 * {@code User-Agent} header in the {@link HttpServletRequest} object. The
 * comparison of browser names is case-insensitive, except for the regular
 * expression comparison. This {@link Conditionlet} provides a drop-down menu
 * with the available comparison mechanisms, and a single text field to enter
 * the value to compare.
 * <p>
 * The format of the {@code User-Agent} is not standardized (basically free
 * format), which makes it difficult to decipher it. This conditionlet uses a
 * Java API called <a
 * href="http://www.bitwalker.eu/software/user-agent-utils">User Agent Utils</a>
 * which parses HTTP requests in real time and gather information about the user
 * agent, detecting a high amount of browsers, browser types, operating systems,
 * device types, rendering engines, and Web applications.
 * </p>
 * <p>
 * The User Agent Utils API uses regular expressions to extract the browser's
 * name from the {@code User-Agent} header. Given that the format is not
 * standard, the name might also contain version numbers. For example, the API
 * can return values like "Chrome", "Safari 7", "Firefox 37",
 * "Internet Explorer 11", and so on. Therefore, if you need to validate against
 * a browser in general without considering versions, say "Firefox", you can
 * select the "Contains" comparison to just lookup the word "Firefox".
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-21-2015
 *
 */
public class UsersBrowserConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "browser";
	private static final String CONDITIONLET_NAME = "User's Browser";

	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";
	private static final String COMPARISON_STARTSWITH = "startsWith";
	private static final String COMPARISON_ENDSWITH = "endsWith";
	private static final String COMPARISON_CONTAINS = "contains";
	private static final String COMPARISON_REGEX = "regex";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	public UsersBrowserConditionlet() {
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
				&& !UtilMethods.isSet(comparisonId)) {
			return false;
		}
		String userAgentInfo = request.getHeader("User-Agent");
		UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
		String browserName = null;
		if (agent != null && agent.getBrowser() != null) {
			browserName = agent.getBrowser().getName();
		}
		if (!UtilMethods.isSet(browserName)) {
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
		if (!comparison.getId().equals(COMPARISON_REGEX)) {
			inputValue = inputValue.toLowerCase();
			browserName = browserName.toLowerCase();
		}
		if (comparison.getId().equals(COMPARISON_IS)) {
			if (browserName.equals(inputValue)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
			if (!browserName.equals(inputValue)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
			if (browserName.startsWith(inputValue)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
			if (browserName.endsWith(inputValue)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
			if (browserName.contains(inputValue)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
			Pattern pattern = Pattern.compile(inputValue);
			Matcher matcher = pattern.matcher(browserName);
			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

}
