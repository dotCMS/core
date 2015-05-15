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

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the referring URL where the
 * user request came from.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-22-2015
 *
 */
public class UsersReferringUrlConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "referring-url";
	private static final String CONDITIONLET_NAME = "User's Referring URL";
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
			String referrerUrl = HttpRequestDataUtil.getReferrerUrl(request);
			if (UtilMethods.isSet(referrerUrl)) {
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
					if (comparison.getId().equals(COMPARISON_IS)) {
						if (inputValue.equalsIgnoreCase(referrerUrl)) {
							result = true;
						}
					} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
						if (!inputValue.equalsIgnoreCase(referrerUrl)) {
							result = true;
						}
					} else if (comparison.getId().startsWith(
							COMPARISON_STARTSWITH)) {
						if (inputValue.startsWith(referrerUrl)) {
							result = true;
						}
					} else if (comparison.getId().endsWith(COMPARISON_ENDSWITH)) {
						if (inputValue.endsWith(referrerUrl)) {
							result = true;
						}
					} else if (comparison.getId().endsWith(COMPARISON_CONTAINS)) {
						if (inputValue.contains(referrerUrl)) {
							result = true;
						}
					} else if (comparison.getId().endsWith(COMPARISON_REGEX)) {
						Pattern pattern = Pattern.compile(inputValue);
						Matcher matcher = pattern.matcher(referrerUrl);
						if (matcher.find()) {
							result = true;
						}
					}
				}
			}
		}
		return result;
	}

}
