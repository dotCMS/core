package com.dotmarketing.portlets.rules.conditionlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the language a user has set
 * in their request. The language selected by the user is in the
 * {@link HttpServletRequest} object, which is used to perform the validation
 * and is retrieved using our own API. This {@link Conditionlet} provides a
 * drop-down menu with the available comparison mechanisms, and a drop-down menu
 * where users can select one or more languages to compare.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-17-2015
 *
 */
public class UsersLanguageConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "language";
	private static final String CONDITIONLET_NAME = "User's Language";

	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	public UsersLanguageConditionlet() {
		super(CONDITIONLET_NAME);
	}

	@Override
	public Set<Comparison> getComparisons() {
		if (this.comparisons == null) {
			this.comparisons = new LinkedHashSet<Comparison>();
			this.comparisons.add(new Comparison(COMPARISON_IS, "Is"));
			this.comparisons.add(new Comparison(COMPARISON_ISNOT, "Is Not"));
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
			if (this.inputValues == null
					|| this.inputValues.get(inputId) == null) {
				getInputs(comparisonId);
			}
			ConditionletInput inputField = this.inputValues.get(inputId);
			validationResult.setConditionletInputId(inputId);
			Set<EntryOption> inputOptions = inputField.getData();
			for (EntryOption option : inputOptions) {
				if (option.getId().equals(selectedValue)) {
					validationResult.setValid(true);
					break;
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
			// Set field configuration and available options
			inputField.setId(INPUT_ID);
			inputField.setMultipleSelectionAllowed(true);
			inputField.setDefaultValue("en");
			inputField.setMinNum(1);
			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
			options.add(new EntryOption("af", "Afrikaans"));
			options.add(new EntryOption("sq", "Albanian"));
			options.add(new EntryOption("ar", "Arabic"));
			options.add(new EntryOption("be", "Belarusian"));
			options.add(new EntryOption("bn", "Bengali, Bangla"));
			options.add(new EntryOption("bs", "Bosnian"));
			options.add(new EntryOption("bg", "Bulgarian"));
			options.add(new EntryOption("ca", "Catalan"));
			options.add(new EntryOption("zh", "Chinese"));
			options.add(new EntryOption("hr", "Croatian"));
			options.add(new EntryOption("cs", "Czech"));
			options.add(new EntryOption("da", "Danish"));
			options.add(new EntryOption("nl", "Dutch"));
			options.add(new EntryOption("en", "English"));
			options.add(new EntryOption("fi", "Finnish"));
			options.add(new EntryOption("fr", "French"));
			options.add(new EntryOption("de", "German"));
			options.add(new EntryOption("el", "Greek"));
			options.add(new EntryOption("ht", "Haitian, Haitian Creole"));
			options.add(new EntryOption("he", "Hebrew (modern)"));
			options.add(new EntryOption("hi", "Hindi"));
			options.add(new EntryOption("hu", "hungarian"));
			options.add(new EntryOption("id", "Indonesian"));
			options.add(new EntryOption("is", "Islandic"));
			options.add(new EntryOption("it", "Italian"));
			options.add(new EntryOption("ja", "Japanese"));
			options.add(new EntryOption("ko", "Korean"));
			options.add(new EntryOption("ku", "Kurdish"));
			options.add(new EntryOption("lt", "Lithuanian"));
			options.add(new EntryOption("no", "Norwegian"));
			options.add(new EntryOption("fa", "Persian (Farsi)"));
			options.add(new EntryOption("pl", "Polish"));
			options.add(new EntryOption("pt", "Portuguese"));
			options.add(new EntryOption("ro", "Romanian"));
			options.add(new EntryOption("ru", "Russian"));
			options.add(new EntryOption("sd", "Sindhi"));
			options.add(new EntryOption("sm", "Samoan"));
			options.add(new EntryOption("sr", "Serbian"));
			options.add(new EntryOption("sk", "Slovak"));
			options.add(new EntryOption("es", "Spanish"));
			options.add(new EntryOption("sv", "Swedish"));
			options.add(new EntryOption("th", "Thai"));
			options.add(new EntryOption("tr", "Turkish"));
			options.add(new EntryOption("uk", "Ukranian"));
			options.add(new EntryOption("vi", "Vietnamese"));
			options.add(new EntryOption("yi", "Yiddish"));
			options.add(new EntryOption("za", "Zhuang, Chuang"));
			inputField.setData(options);
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
		String language = WebAPILocator.getLanguageWebAPI()
				.getLanguage(request).getLanguageCode();
		if (!UtilMethods.isSet(language)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(INPUT_ID, value
					.getValue()));
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		if (comparison.getId().equals(COMPARISON_IS)) {
			for (ConditionValue value : values) {
				if (value.getValue().equalsIgnoreCase(language)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
			for (ConditionValue value : values) {
				if (value.getValue().equalsIgnoreCase(language)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
