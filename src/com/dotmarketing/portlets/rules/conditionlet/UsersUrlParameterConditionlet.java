package com.dotmarketing.portlets.rules.conditionlet;

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
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the value of a specific
 * parameter in the URL (a query String parameter).
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-12-2015
 *
 */
public class UsersUrlParameterConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT1_ID = "urlparam-name";
	private static final String INPUT2_ID = "urlparam-value";
	private static final String CONDITIONLET_NAME = "User's URL Parameter";
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
			if (UtilMethods.isSet(selectedValue)) {
				validationResult.setValid(true);
			} else {
				validationResult.setErrorMessage("Invalid value for input '"
						+ inputValue.getConditionletInputId() + "': '"
						+ selectedValue + "'");
			}
		}
		return validationResult;
	}

	@Override
	public Collection<ConditionletInput> getInputs(String comparisonId) {
		if (this.inputValues == null) {
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			ConditionletInput inputField1 = new ConditionletInput();
			// Set field #1 configuration and available options
			inputField1.setId(INPUT1_ID);
			inputField1.setUserInputAllowed(true);
			inputField1.setMultipleSelectionAllowed(false);
			this.inputValues.put(inputField1.getId(), inputField1);
			ConditionletInput inputField2 = new ConditionletInput();
			// Set field #2 configuration and available options
			inputField2.setId(INPUT2_ID);
			inputField2.setUserInputAllowed(true);
			inputField2.setMultipleSelectionAllowed(false);
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			this.inputValues.put(inputField2.getId(), inputField2);
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
		Map<String, String> conditionletValues = new HashMap<String, String>();
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(value.getId(), value
					.getValue()));
			conditionletValues.put(value.getId(), value.getValue());
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		String urlParamValue = HttpRequestDataUtil.getUrlParameterValue(
				request, conditionletValues.get(INPUT1_ID));
		if (!UtilMethods.isSet(urlParamValue)) {
			return false;
		}
		if (comparison.getId().equals(COMPARISON_IS)) {
			if (urlParamValue.equalsIgnoreCase(conditionletValues
					.get(INPUT2_ID))) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
			if (!urlParamValue.equalsIgnoreCase(conditionletValues
					.get(INPUT2_ID))) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_STARTSWITH)) {
			if (urlParamValue.startsWith(conditionletValues.get(INPUT2_ID))) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_ENDSWITH)) {
			if (urlParamValue.endsWith(conditionletValues.get(INPUT2_ID))) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_CONTAINS)) {
			if (urlParamValue.contains(conditionletValues.get(INPUT2_ID))) {
				return true;
			}
		} else if (comparison.getId().endsWith(COMPARISON_REGEX)) {
			Pattern pattern = Pattern.compile(urlParamValue);
			Matcher matcher = pattern
					.matcher(conditionletValues.get(INPUT2_ID));
			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

}
