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
// * This conditionlet will allow CMS users to check the IP address of the user
// * that issued the request. The information is obtained by reading a specific
// * list of headers in the {@link HttpServletRequest} object that might contain
// * the actual IP address. The address can be compared with other specified IP
// * addresses and also with a specific netmask. This {@link Conditionlet}
// * provides a drop-down menu with the available comparison mechanisms, and a
// * single text field to enter the value to compare.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 04-21-2015
// *
// */
//public class UsersIpAddressConditionlet extends Conditionlet<UsersIpAddressConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "ip-address";
//	private static final String CONDITIONLET_NAME = "User's IP Address";
//
//	private static final String COMPARISON_IS = "is";
//	private static final String COMPARISON_ISNOT = "isNot";
//	private static final String COMPARISON_STARTSWITH = "startsWith";
//	private static final String COMPARISON_NETMASK = "netmask";
//	private static final String COMPARISON_REGEX = "regex";
//
//	private LinkedHashSet<Comparison> comparisons = null;
//	private Map<String, ConditionletInput> inputValues = null;
//
//	public UsersIpAddressConditionlet() {
//        super("api.system.ruleengine.conditionlet.VisitorIpAddress", ImmutableSet.<Comparison>of(IS,
//                                                                                              Comparison.IS_NOT,
//                                                                                              Comparison.STARTS_WITH,
//                                                                                              Comparison.NETMASK,
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
//					|| comparisonId.equals(COMPARISON_STARTSWITH)) {
//				if (UtilMethods.isSet(selectedValue)) {
//					validationResult.setValid(true);
//				}
//			} else if (comparisonId.equals(COMPARISON_NETMASK)) {
//				if (selectedValue.contains("/")) {
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
////        String ipAddress = null;
////		try {
////			InetAddress address = HttpRequestDataUtil.getIpAddress(request);
////			ipAddress = address.getHostAddress();
////		} catch (UnknownHostException e) {
////			Logger.error(this,
////					"Could not retrieved a valid IP address from request: "
////							+ request.getRequestURL());
////		}
////		if (!UtilMethods.isSet(ipAddress)) {
////			return false;
////		}
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		String inputValue = values.get(0).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			if (ipAddress.equals(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
////			if (!ipAddress.equals(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().startsWith(COMPARISON_STARTSWITH)) {
////			if (ipAddress.startsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().startsWith(COMPARISON_NETMASK)) {
////			if (HttpRequestDataUtil.isIpMatchingNetmask(ipAddress, inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().endsWith(COMPARISON_REGEX)) {
////			Pattern pattern = Pattern.compile(inputValue);
////			Matcher matcher = pattern.matcher(ipAddress);
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
