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
// * This conditionlet will allow CMS users to check the URL of the first page
// * that a user has visited in its current session. The comparison of URLs is
// * case-insensitive, except for the regular expression comparison. This
// * {@link Conditionlet} provides a drop-down menu with the available comparison
// * mechanisms, and a single text field to enter the value to compare.
// * <p>
// * This conditionlet uses Clickstream, which is a user tracking component for
// * Java web applications, and it must be enabled in your dotCMS configuration.
// * To do it, just go to the {@code dotmarketing-config.properties} file, look
// * for a property called {@code ENABLE_CLICKSTREAM_TRACKING} and set its value to
// * {@code true}. You can also take a look at the other Clickstream configuration
// * properties to have it running according to your environment's resources.
// * </p>
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-13-2015
// *
// */
//public class UsersLandingPageUrlConditionlet extends Conditionlet<UsersLandingPageUrlConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "landing-url";
//	private static final String CONDITIONLET_NAME = "User's Landing Page URL";
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
//	public UsersLandingPageUrlConditionlet() {
//        super("api.system.ruleengine.conditionlet.VisitorsLandingPage", ImmutableSet.<Comparison>of(IS,
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
//			ConditionletInput inputField = new ConditionletInput();
//			inputField.setId(INPUT_ID);
//			inputField.setUserInputAllowed(true);
//			inputField.setMultipleSelectionAllowed(false);
//			inputField.setMinNum(2);
//			this.inputValues.put(inputField.getId(), inputField);
//		}
//		return this.inputValues.values();
//	}
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
//
////        Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		String conditionletValue = values.get(0).getValue();
////		inputValues
////				.add(new ConditionletInputValue(INPUT_ID, conditionletValue));
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		Clickstream clickstream = (Clickstream) request.getSession(true)
////				.getAttribute("clickstream");
////		if (clickstream == null) {
////			return false;
////		}
////		String firstUrl = null;
////		List<ClickstreamRequest> clickstreamRequests = clickstream
////				.getClickstreamRequests();
////		if (clickstreamRequests != null && clickstreamRequests.size() > 0) {
////			firstUrl = clickstreamRequests.get(0).getRequestURI();
////		}
////		if (!UtilMethods.isSet(firstUrl)) {
////			return false;
////		}
////		if (!comparison.getId().equals(COMPARISON_REGEX)) {
////			firstUrl = firstUrl.toLowerCase();
////			conditionletValue = conditionletValue.toLowerCase();
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			if (firstUrl.equalsIgnoreCase(conditionletValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			if (!firstUrl.equalsIgnoreCase(conditionletValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
////			if (firstUrl.startsWith(conditionletValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
////			if (firstUrl.endsWith(conditionletValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
////			if (firstUrl.contains(conditionletValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
////			Pattern pattern = Pattern.compile(conditionletValue);
////			Matcher matcher = pattern.matcher(firstUrl);
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
