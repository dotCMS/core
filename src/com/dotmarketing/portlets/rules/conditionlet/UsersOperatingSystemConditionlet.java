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
// * This conditionlet will allow CMS users to check the Operating System of the
// * user that issued the request. The information is obtained by reading the
// * {@code User-Agent} header in the {@link HttpServletRequest} object. The
// * comparison of operating system names is case-insensitive, except for the
// * regular expression comparison. This {@link Conditionlet} provides a drop-down
// * menu with the available comparison mechanisms, and a single text field to
// * enter the value to compare.
// * <p>
// * The format of the {@code User-Agent} is not standardized (basically free
// * format), which makes it difficult to decipher it. This conditionlet uses a
// * Java API called <a
// * href="http://www.bitwalker.eu/software/user-agent-utils">User Agent Utils</a>
// * which parses HTTP requests in real time and gather information about the user
// * agent, detecting a high amount of browsers, browser types, operating systems,
// * device types, rendering engines, and Web applications.
// * </p>
// * <p>
// * The User Agent Utils API uses regular expressions to extract the browser's
// * name from the {@code User-Agent} header. Given that the format is not
// * standard, the name might also contain version numbers. Therefore, if you need
// * to validate against an operating system in general without considering
// * versions, say "Mac", you can select the "Contains" or "Starts With"
// * comparison to just lookup the word "Mac".
// * </p>
// *
// * @author Jose Castro
// * @version 1.0
// * @since 04-21-2015
// *
// */
//public class UsersOperatingSystemConditionlet extends Conditionlet<UsersOperatingSystemConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "os";
//	private static final String CONDITIONLET_NAME = "User's Operating System";
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
//	public UsersOperatingSystemConditionlet() {
//        super("api.ruleengine.system.conditionlet.HasVisitedUrl", ImmutableSet.<Comparison>of(IS,
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
//
////        String userAgentInfo = request.getHeader("User-Agent");
////		UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
////		String osName = null;
////		if (agent != null && agent.getOperatingSystem() != null) {
////			osName = agent.getOperatingSystem().getName();
////		}
////		if (!UtilMethods.isSet(osName)) {
////			return false;
////		}
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		String inputValue = values.get(0).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		if (!comparison.getId().endsWith(COMPARISON_REGEX)) {
////			inputValue = inputValue.toLowerCase();
////			osName = osName.toLowerCase();
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			if (osName.equalsIgnoreCase(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			if (!osName.equalsIgnoreCase(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
////			if (osName.startsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
////			if (osName.endsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
////			if (osName.contains(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
////			Pattern pattern = Pattern.compile(inputValue);
////			Matcher matcher = pattern.matcher(osName);
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
