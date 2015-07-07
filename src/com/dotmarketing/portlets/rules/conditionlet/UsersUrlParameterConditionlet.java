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
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow dotCMS users to check the value of a specific
 * parameter in the URL (a query String parameter). The comparison of parameter
 * names and values is case-insensitive, except for the regular expression
 * comparison. This {@link Conditionlet} provides a drop-down menu with the
 * available comparison mechanisms, and a text field to enter the name of the
 * parameter, and text field for the value to it.
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

	public UsersUrlParameterConditionlet() {
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
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			ConditionletInput inputField1 = new ConditionletInput();
			// Set field #1 configuration and available options
			inputField1.setId(INPUT1_ID);
			inputField1.setUserInputAllowed(true);
			inputField1.setMultipleSelectionAllowed(false);
			inputField1.setMinNum(1);
			this.inputValues.put(inputField1.getId(), inputField1);
			ConditionletInput inputField2 = new ConditionletInput();
			// Set field #2 configuration and available options
			inputField2.setId(INPUT2_ID);
			inputField2.setUserInputAllowed(true);
			inputField2.setMultipleSelectionAllowed(false);
			inputField1.setMinNum(1);
			this.inputValues.put(inputField2.getId(), inputField2);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		if (!UtilMethods.isSet(values) || values.size() < 2
				|| !UtilMethods.isSet(comparisonId)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		Map<String, String> conditionletValues = new HashMap<String, String>();
		String inputValue1 = values.get(0).getValue();
		String inputValue2 = values.get(1).getValue();
		inputValues.add(new ConditionletInputValue(INPUT1_ID, inputValue1));
		conditionletValues.put(INPUT1_ID, inputValue1);
		inputValues.add(new ConditionletInputValue(INPUT2_ID, inputValue2));
		conditionletValues.put(INPUT2_ID, inputValue2);
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		String urlParamValue = HttpRequestDataUtil.getUrlParameterValue(
				request, inputValue1);
		if (!UtilMethods.isSet(urlParamValue)) {
			return false;
		}
		if (!comparison.getId().equals(COMPARISON_REGEX)) {
			urlParamValue = urlParamValue.toLowerCase();
			inputValue2 = inputValue2.toLowerCase();
		}
		if (comparison.getId().equals(COMPARISON_IS)) {
			if (urlParamValue.equalsIgnoreCase(inputValue2)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
			if (!urlParamValue.equalsIgnoreCase(inputValue2)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
			if (urlParamValue.startsWith(inputValue2)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
			if (urlParamValue.endsWith(inputValue2)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
			if (urlParamValue.contains(inputValue2)) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
			Pattern pattern = Pattern.compile(inputValue2);
			Matcher matcher = pattern.matcher(urlParamValue);
			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

}
