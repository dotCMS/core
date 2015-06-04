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

import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the value of any of the HTTP
 * headers sent with the every {@link HttpServletRequest} object.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-13-2015
 *
 */
public class UsersBrowserHeaderConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT1_ID = "browser-header";
	private static final String INPUT2_ID = "header-value";
	private static final String CONDITIONLET_NAME = "User's Browser Header";
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
			ConditionletInput inputField = this.inputValues.get(inputId);
			validationResult.setConditionletInputId(inputId);
			if (INPUT1_ID.equalsIgnoreCase(inputId)) {
				Set<EntryOption> inputOptions = inputField.getData();
				if (inputOptions != null) {
					for (EntryOption option : inputOptions) {
						if (option.getId().equalsIgnoreCase(selectedValue)) {
							validationResult.setValid(true);
							break;
						}
					}
				}
			} else {
				if (UtilMethods.isSet(selectedValue)) {
					validationResult.setValid(true);
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
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			// Set field #1 configuration and available options
			ConditionletInput inputField = new ConditionletInput();
			inputField.setId(INPUT1_ID);
			inputField.setMultipleSelectionAllowed(true);
			inputField.setDefaultValue("");
			inputField.setMinNum(1);
			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
			options.add(new EntryOption("Accept", "Accept"));
			options.add(new EntryOption("Accept-Charset", "Accept-Charset"));
			options.add(new EntryOption("Accept-Encoding", "Accept-Encoding"));
			options.add(new EntryOption("Accept-Language", "Accept-Language"));
			options.add(new EntryOption("Accept-Datetime", "Accept-Datetime"));
			options.add(new EntryOption("Authorization", "Authorization"));
			options.add(new EntryOption("Cache-Control", "Cache-Control"));
			options.add(new EntryOption("Connection", "Connection"));
			options.add(new EntryOption("Cookie", "Cookie"));
			options.add(new EntryOption("Content-Length", "Content-Length"));
			options.add(new EntryOption("Content-MD5", "Content-MD5"));
			options.add(new EntryOption("Content-Type", "Content-Type"));
			options.add(new EntryOption("Date", "Date"));
			options.add(new EntryOption("Expect", "Expect"));
			options.add(new EntryOption("From", "From"));
			options.add(new EntryOption("Host", "Host"));
			options.add(new EntryOption("If-Match", "If-Match"));
			options.add(new EntryOption("If-Modified-Since",
					"If-Modified-Since"));
			options.add(new EntryOption("If-None-Match", "If-None-Match"));
			options.add(new EntryOption("If-Range", "If-Range"));
			options.add(new EntryOption("If-Unmodified-Since",
					"If-Unmodified-Since"));
			options.add(new EntryOption("Max-Forwards", "Max-Forwards"));
			options.add(new EntryOption("Origin", "Origin"));
			options.add(new EntryOption("Pragma", "Pragma"));
			options.add(new EntryOption("Proxy-Authorization",
					"Proxy-Authorization"));
			options.add(new EntryOption("Range", "Range"));
			options.add(new EntryOption("Referer", "Referer"));
			options.add(new EntryOption("TE", "TE"));
			options.add(new EntryOption("User-Agent", "User-Agent"));
			options.add(new EntryOption("Upgrade", "Upgrade"));
			options.add(new EntryOption("Via", "Via"));
			options.add(new EntryOption("Warning", "Warning"));
			options.add(new EntryOption("X-Requested-With", "X-Requested-With"));
			options.add(new EntryOption("DNT", "DNT"));
			options.add(new EntryOption("X-Forwarded-For", "X-Forwarded-For"));
			options.add(new EntryOption("X-Forwarded-Host", "X-Forwarded-Host"));
			options.add(new EntryOption("Front-End-Https", "Front-End-Https"));
			options.add(new EntryOption("X-Http-Method-Override",
					"X-Http-Method-Override"));
			options.add(new EntryOption("X-ATT-DeviceId", "X-ATT-DeviceId"));
			options.add(new EntryOption("X-Wap-Profile", "X-Wap-Profile"));
			options.add(new EntryOption("Proxy-Connection", "Proxy-Connection"));
			options.add(new EntryOption("X-UIDH", "X-UIDH"));
			options.add(new EntryOption("X-Csrf-Token", "X-Csrf-Token"));
			inputField.setData(options);
			this.inputValues.put(inputField.getId(), inputField);
			// Set field #2 configuration and available options
			ConditionletInput inputField2 = new ConditionletInput();
			inputField2.setId(INPUT2_ID);
			inputField2.setMultipleSelectionAllowed(false);
			inputField2.setDefaultValue("");
			this.inputValues.put(inputField2.getId(), inputField2);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		boolean result = false;
		if (!UtilMethods.isSet(values) && values.size() == 0
				&& !UtilMethods.isSet(comparisonId)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		List<String> selectedHeaders = new ArrayList<String>();
		String headerValue = null;
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(value.getId(), value
					.getValue()));
			if (INPUT1_ID.equalsIgnoreCase(value.getId())) {
				selectedHeaders.add(value.getValue());
			} else {
				headerValue = value.getValue();
			}
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (!validationResults.hasErrors()) {
			return evaluateInput(request, comparison, selectedHeaders,
					headerValue);
		}
		return false;
	}

	/**
	 * Evaluates the list of values entered in the conditionlet with the headers
	 * in the request object.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param comparison
	 *            - The {@link Comparison} mechanism for the values.
	 * @param selectedHeaders
	 *            - The list of selected headers specified in the conditionlet
	 * @param conditionletInput
	 *            - The value that matches the selected header(s).
	 * @return If the input value matches the header(s) values(2), returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean evaluateInput(HttpServletRequest request,
			Comparison comparison, List<String> selectedHeaders,
			String conditionletInput) {
		boolean result = false;
		for (String headerName : selectedHeaders) {
			String headerValue = request.getHeader(headerName);
			if (!UtilMethods.isSet(headerValue)) {
				return false;
			}
			result = compare(comparison, headerValue, conditionletInput);
			if (!result) {
				break;
			}
		}
		return result;
	}

	/**
	 * Compares the value of a header with the specified value in the
	 * conditionlet according to the {@link Comparison} mechanism.
	 * 
	 * @param comparison
	 *            - The {@link Comparison} method.
	 * @param headerValue
	 *            - The value of the request header.
	 * @param conditionletInput
	 *            - The validation input in the conditionlet.
	 * @return If the input value matches the header value, returns {@code true}
	 *         . Otherwise, returns {@code false}.
	 */
	private boolean compare(Comparison comparison, String headerValue,
			String conditionletInput) {
		if (comparison.getId().equals(COMPARISON_IS)) {
			if (headerValue.equalsIgnoreCase(conditionletInput)) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
			if (!headerValue.equalsIgnoreCase(conditionletInput)) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_STARTSWITH)) {
			if (headerValue.startsWith(conditionletInput)) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_ENDSWITH)) {
			if (headerValue.endsWith(conditionletInput)) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_CONTAINS)) {
			if (headerValue.contains(conditionletInput)) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_REGEX)) {
			Pattern pattern = Pattern.compile(conditionletInput);
			Matcher matcher = pattern.matcher(headerValue);
			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

}
