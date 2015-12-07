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
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.dotmarketing.util.UtilMethods;
//
//import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
//
///**
// * This conditionlet will allow dotCMS users to check the client's current date
// * and time when a page was requested. This {@link Conditionlet} provides a
// * drop-down menu with the available comparison mechanisms, and a text field to
// * enter the date and time to compare. This date/time parameter will be
// * expressed in milliseconds in order to avoid any format-related and time zone
// * issues.
// * <p>
// * The date and time of the request is determined by the IP address of the
// * client that issued the request. Geographic information is then retrieved via
// * the <a href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java
// * API</a>.
// * </p>
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-07-2015
// *
// */
//public class UsersDateTimeConditionlet extends Conditionlet<UsersDateTimeConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "datetime";
//	private static final String CONDITIONLET_NAME = "User's Date & Time";
//
//	private Map<String, ConditionletInput> inputValues = null;
//
//	public UsersDateTimeConditionlet() {
//		super(CONDITIONLET_NAME, ImmutableSet.of(EQUAL,
//                                                 Comparison.LESS_THAN,
//                                                 Comparison.GREATER_THAN,
//                                                 Comparison.LESS_THAN_OR_EQUAL,
//                                                 Comparison.GREATER_THAN_OR_EQUAL), Sets.newHashSet());
//	}
//
//	protected ValidationResult validate(Comparison comparison,
//			ConditionletInputValue inputValue) {
//		ValidationResult validationResult = new ValidationResult();
//		String inputId = inputValue.getConditionletInputId();
//		if (UtilMethods.isSet(inputId)) {
//			String selectedValue = inputValue.getValue();
//			// Number of digits in long number ranges from 1 to 19
//			if (Pattern.matches("\\d{1,19}", selectedValue)) {
//				validationResult.setValid(true);
//			} else {
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
////		long clientDateTime = 0;
////		try {
////			InetAddress ip = HttpRequestDataUtil.getIpAddress(request);
////			String ipAddress = ip.getHostAddress();
////			// TODO: Remove
////			ipAddress = "170.123.234.133";
////			Calendar date = GeoIp2CityDbUtil.getInstance().getDateTime(
////					ipAddress);
////			clientDateTime = date.getTime().getTime();
////		} catch (IOException | GeoIp2Exception e) {
////			Logger.error(
////					this,
////					"Could not retrieved a valid date from request: "
////							+ request.getRequestURL());
////		}
////		if (clientDateTime == 0) {
////			return false;
////		}
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		String inputValue = values.get(0).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		long conditionletInput = Long.parseLong(inputValue);
////		if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
////			if (clientDateTime > conditionletInput) {
////				return true;
////			}
////		}
////		if (comparison.getId().equals(COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
////			if (clientDateTime >= conditionletInput) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_EQUAL_TO)) {
////			if (clientDateTime == conditionletInput) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
////			if (clientDateTime <= conditionletInput) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN)) {
////			if (clientDateTime < conditionletInput) {
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
