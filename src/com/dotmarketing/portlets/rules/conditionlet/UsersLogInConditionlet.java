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
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * This conditionlet will allow CMS users to check whether the user that issued
 * the request is currently logged in or not.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-14-2015
 *
 */
public class UsersLogInConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "user-logged-in";
	private static final String CONDITIONLET_NAME = "Current User's Log In Status";

	private static final String ENTRYOPTION_1 = "is";
	private static final String ENTRYOPTION_2 = "isNot";

	private Map<String, ConditionletInput> inputValues = null;

	@Override
	protected String getName() {
		return CONDITIONLET_NAME;
	}

	@Override
	public Set<Comparison> getComparisons() {
		return null;
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
			ConditionletInput inputField = this.inputValues.get(inputId);
			validationResult.setConditionletInputId(inputId);
			Set<EntryOption> inputOptions = inputField.getData();
			if (inputOptions != null) {
				for (EntryOption option : inputOptions) {
					if (option.getId().equalsIgnoreCase(selectedValue)) {
						validationResult.setValid(true);
						break;
					}
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
			inputField.setId(INPUT_ID);
			inputField.setMultipleSelectionAllowed(false);
			inputField.setDefaultValue("");
			inputField.setMinNum(1);
			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
			options.add(new EntryOption(ENTRYOPTION_1, "Is Logged In"));
			options.add(new EntryOption(ENTRYOPTION_2, "Is Not Logged In"));
			inputField.setData(options);
			this.inputValues.put(inputField.getId(), inputField);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		if (!UtilMethods.isSet(values) && values.size() == 0) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		boolean mustBeLoggedIn = false;
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(value.getId(), value
					.getValue()));
			if (ENTRYOPTION_1.equalsIgnoreCase(value.getValue())) {
				mustBeLoggedIn = true;
			}
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		try {
			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			if ((mustBeLoggedIn && user != null)
					|| (!mustBeLoggedIn && user == null)) {
				return true;
			}
		} catch (DotRuntimeException | PortalException | SystemException e) {
			Logger.error(this,
					"Could not retrieved logged-in user from request: "
							+ request.getRequestURL());
		}
		return false;
	}

}
