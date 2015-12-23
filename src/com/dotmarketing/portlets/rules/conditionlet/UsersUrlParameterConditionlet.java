//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.ValidationResult;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import java.util.Collection;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.dotmarketing.util.Logger;
//import com.dotmarketing.util.UtilMethods;
//
//import static com.dotmarketing.portlets.rules.conditionlet.Comparison.IS;
//
///**
// * This conditionlet will allow dotCMS users to check the value of a specific
// * parameter in the URL (a query String parameter). The comparison of parameter
// * names and values is case-insensitive, except for the regular expression
// * comparison. This {@link Conditionlet} provides a drop-down menu with the
// * available comparison mechanisms, and a text field to enter the name of the
// * parameter, and text field for the value to it.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-12-2015
// *
// */
//public class UsersUrlParameterConditionlet extends Conditionlet<UsersUrlParameterConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT1_ID = "urlparam-name";
//	private static final String INPUT2_ID = "urlparam-value";
//	private static final String CONDITIONLET_NAME = "User's URL Parameter";
//
//	private static final String COMPARISON_IS = "is";
//	private static final String COMPARISON_ISNOT = "isNot";
//	private static final String COMPARISON_STARTSWITH = "startsWith";
//	private static final String COMPARISON_ENDSWITH = "endsWith";
//	private static final String COMPARISON_CONTAINS = "contains";
//	private static final String COMPARISON_REGEX = "regex";
//
//	private LinkedHashSet<Comparison> comparisons = null;
//	private Map<String, ConditionletInput> inputValues = null;
//
//	public UsersUrlParameterConditionlet() {
//        super("api.ruleengine.system.conditionlet.RequestUrlParameter", ImmutableSet.<Comparison>of(IS,
//                                                                                              Comparison.IS_NOT,
//                                                                                              Comparison.STARTS_WITH,
//                                                                                              Comparison.ENDS_WITH,
//                                                                                              Comparison.CONTAINS,
//                                                                                              Comparison.REGEX), parameters);
//	}
//
//	protected ValidationResult validate(Comparison comparison,
//			ConditionletInputValue inputValue) {
//		ValidationResult validationResult = new ValidationResult();
//		String inputId = inputValue.getConditionletInputId();
//		if (UtilMethods.isSet(inputId)) {
//			String selectedValue = inputValue.getValue();
//			String comparisonId = comparison.getId();
//			if (comparisonId.equals(COMPARISON_IS)
//					|| comparisonId.equals(COMPARISON_ISNOT)
//					|| comparisonId.equals(COMPARISON_STARTSWITH)
//					|| comparisonId.equals(COMPARISON_ENDSWITH)
//					|| comparisonId.equals(COMPARISON_CONTAINS)) {
//				if (UtilMethods.isSet(selectedValue)) {
//					validationResult.setValid(true);
//				}
//			} else if (comparisonId.equals(COMPARISON_REGEX)) {
//				try {
//					Pattern.compile(selectedValue);
//					validationResult.setValid(true);
//				} catch (PatternSyntaxException e) {
//					Logger.debug(this, "Invalid RegEx " + selectedValue);
//				}
//			}
//			if (!validationResult.isValid()) {
//				validationResult.setErrorMessage("Invalid value for input '"
//						+ inputId + "': '" + selectedValue + "'");
//			}
//		}
//		return validationResult;
//	}
//
//	@Override
//	public Collection<ConditionletInput> getInputs(String comparisonId) {
//		if (this.inputValues == null) {
//			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
//			ConditionletInput inputField1 = new ConditionletInput();
//			// Set field #1 configuration and available options
//			inputField1.setId(INPUT1_ID);
//			inputField1.setUserInputAllowed(true);
//			inputField1.setMultipleSelectionAllowed(false);
//			inputField1.setMinNum(1);
//			this.inputValues.put(inputField1.getId(), inputField1);
//			ConditionletInput inputField2 = new ConditionletInput();
//			// Set field #2 configuration and available options
//			inputField2.setId(INPUT2_ID);
//			inputField2.setUserInputAllowed(true);
//			inputField2.setMultipleSelectionAllowed(false);
//			inputField1.setMinNum(1);
//			this.inputValues.put(inputField2.getId(), inputField2);
//		}
//		return this.inputValues.values();
//	}
//
//	@Override
//	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		Map<String, String> conditionletValues = new HashMap<String, String>();
////		String inputValue1 = values.get(0).getValue();
////		String inputValue2 = values.get(1).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT1_ID, inputValue1));
////		conditionletValues.put(INPUT1_ID, inputValue1);
////		inputValues.add(new ConditionletInputValue(INPUT2_ID, inputValue2));
////		conditionletValues.put(INPUT2_ID, inputValue2);
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		String urlParamValue = HttpRequestDataUtil.getUrlParameterValue(
////				request, inputValue1);
////		if (!UtilMethods.isSet(urlParamValue)) {
////			return false;
////		}
////		if (!comparison.getId().equals(COMPARISON_REGEX)) {
////			urlParamValue = urlParamValue.toLowerCase();
////			inputValue2 = inputValue2.toLowerCase();
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			if (urlParamValue.equalsIgnoreCase(inputValue2)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			if (!urlParamValue.equalsIgnoreCase(inputValue2)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
////			if (urlParamValue.startsWith(inputValue2)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
////			if (urlParamValue.endsWith(inputValue2)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
////			if (urlParamValue.contains(inputValue2)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
////			Pattern pattern = Pattern.compile(inputValue2);
////			Matcher matcher = pattern.matcher(urlParamValue);
////			if (matcher.find()) {
////				return true;
////			}
////		}
//		return false;
//	}
//
//    @Override
//    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
//        return new Instance(comparison, values);
//    }
//
//    public static class Instance implements RuleComponentInstance {
//
//        private Instance(Comparison comparison, List<ParameterModel> values) {
//        }
//    }
//
//}
