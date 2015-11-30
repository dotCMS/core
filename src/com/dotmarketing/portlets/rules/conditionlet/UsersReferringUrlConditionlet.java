package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.ValidationResult;
import com.dotmarketing.portlets.rules.ValidationResults;
import com.dotmarketing.portlets.rules.model.ParameterModel;
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

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import static com.dotmarketing.portlets.rules.conditionlet.Comparison.IS;

/**
 * This conditionlet will allow dotCMS users to check the referring URL where
 * the user request came from. For example, the users will be able to determine
 * if the incoming request came as a result of a Google search, or from a link
 * in a specific Web site, etc. The comparison of URLs is case-insensitive,
 * except for the regular expression comparison. This {@link Conditionlet}
 * provides a drop-down menu with the available comparison mechanisms, and a
 * text field to enter the value to compare.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-22-2015
 *
 */
public class UsersReferringUrlConditionlet extends Conditionlet<UsersReferringUrlConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "referring-url";
	private static final String CONDITIONLET_NAME = "User's Referring URL";

	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";
	private static final String COMPARISON_STARTSWITH = "startsWith";
	private static final String COMPARISON_ENDSWITH = "endsWith";
	private static final String COMPARISON_CONTAINS = "contains";
	private static final String COMPARISON_REGEX = "regex";

	private Map<String, ConditionletInput> inputValues = null;

	public UsersReferringUrlConditionlet() {
        super("api.ruleengine.system.conditionlet.ReferringUrl", ImmutableSet.<Comparison>of(IS,
                                                                                              Comparison.IS_NOT,
                                                                                              Comparison.STARTS_WITH,
                                                                                              Comparison.ENDS_WITH,
                                                                                              Comparison.CONTAINS,
                                                                                              Comparison.REGEX));
	}

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
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {

//        String referrerUrl = HttpRequestDataUtil.getReferrerUrl(request);
//		if (!UtilMethods.isSet(referrerUrl)) {
//			return false;
//		}
//		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
//		String inputValue = values.get(0).getValue();
//		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
//		ValidationResults validationResults = validate(comparison, inputValues);
//		if (validationResults.hasErrors()) {
//			return false;
//		}
//		if (!comparison.getId().equals(COMPARISON_REGEX)) {
//			referrerUrl = referrerUrl.toLowerCase();
//			inputValue = inputValue.toLowerCase();
//		}
//		if (comparison.getId().equals(COMPARISON_IS)) {
//			if (referrerUrl.equalsIgnoreCase(inputValue)) {
//				return true;
//			}
//		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
//			if (!referrerUrl.equalsIgnoreCase(inputValue)) {
//				return true;
//			}
//		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
//			if (referrerUrl.startsWith(inputValue)) {
//				return true;
//			}
//		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
//			if (referrerUrl.endsWith(inputValue)) {
//				return true;
//			}
//		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
//			if (referrerUrl.contains(inputValue)) {
//				return true;
//			}
//		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
//			Pattern pattern = Pattern.compile(inputValue);
//			Matcher matcher = pattern.matcher(referrerUrl);
//			if (matcher.find()) {
//				return true;
//			}
//		}
		return false;
	}

    @Override
    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
        return new Instance(comparison, values);
    }

    public static class Instance implements RuleComponentInstance {

        private Instance(Comparison comparison, List<ParameterModel> values) {
        }
    }

}
