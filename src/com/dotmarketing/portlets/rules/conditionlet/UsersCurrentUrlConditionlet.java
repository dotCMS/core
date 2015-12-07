//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotcms.repackage.com.google.common.collect.Sets;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.ValidationResult;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
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
//import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
//
///**
// * This conditionlet will allow CMS users to check the current URL in a request.
// * The comparison of URLs is case-insensitive, except for the regular expression
// * comparison. This {@link Conditionlet} provides a drop-down menu with the
// * available comparison mechanisms, and a text field to enter the value to
// * compare.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 04-27-2015
// *
// */
//public class UsersCurrentUrlConditionlet extends Conditionlet<UsersCurrentUrlConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "current-url";
//	private static final String CONDITIONLET_NAME = "User's Current URL";
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
//	public UsersCurrentUrlConditionlet() {
//		super(CONDITIONLET_NAME, ImmutableSet.of(IS,
//                                                 Comparison.IS_NOT,
//                                                 Comparison.STARTS_WITH,
//                                                 Comparison.ENDS_WITH,
//                                                 Comparison.CONTAINS,
//                                                 Comparison.REGEX), Sets.newHashSet());
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
//			ConditionletInput inputField = new ConditionletInput();
//			inputField.setId(INPUT_ID);
//			inputField.setUserInputAllowed(true);
//			inputField.setMultipleSelectionAllowed(false);
//			inputField.setMinNum(1);
//			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
//			this.inputValues.put(inputField.getId(), inputField);
//		}
//		return this.inputValues.values();
//	}
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
////        String requestUri = null;
////		try {
////			requestUri = HttpRequestDataUtil.getUri(request);
////		} catch (UnsupportedEncodingException e) {
////			Logger.error(this, "Could not retrieved a valid URI from request: "
////					+ request.getRequestURL());
////		}
////		if (!UtilMethods.isSet(requestUri)) {
////			return false;
////		}
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		String inputValue = values.get(0).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		if (!comparison.getId().equals(COMPARISON_REGEX)) {
////			requestUri = requestUri.toLowerCase();
////			inputValue = inputValue.toLowerCase();
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			if (requestUri.equalsIgnoreCase(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			if (!requestUri.equalsIgnoreCase(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
////			if (requestUri.startsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
////			if (requestUri.endsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
////			if (requestUri.contains(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
////			Pattern pattern = Pattern.compile(inputValue);
////			Matcher matcher = pattern.matcher(requestUri);
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
