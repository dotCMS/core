//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotcms.repackage.com.google.common.collect.Lists;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.ValidationResult;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import java.util.Calendar;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.dotmarketing.util.UtilMethods;
//
//import static com.dotmarketing.portlets.rules.conditionlet.Comparison.EQUAL;
//
///**
// * This conditionlet will allow dotCMS users to check the client's current time
// * when a page was requested. This {@link Conditionlet} provides a drop-down
// * menu with the available comparison mechanisms, and a text field to enter the
// * time to compare. If the selected comparison method is "Between" another
// * textfield will be available for the users to be able to enter a range of
// * times. This time parameter(s) will be expressed in milliseconds in order to
// * avoid any format-related and time zone issues.
// * <p>
// * The time of the request is determined by the IP address of the client that
// * issued the request. Geographic information is then retrieved via the <a
// * href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java API</a>.
// * </p>
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-13-2015
// *
// */
//public class UsersTimeConditionlet extends Conditionlet<UsersTimeConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT1_ID = "time1";
//	private static final String INPUT2_ID = "time2";
//
//	private Map<String, ConditionletInput> inputValuesType1 = null;
//	private Map<String, ConditionletInput> inputValuesType2 = null;
//
//	public UsersTimeConditionlet() {
//        super("api.ruleengine.system.conditionlet.VisitorsLocalTime", ImmutableSet.<Comparison>of(EQUAL,
//                                                                                              Comparison.LESS_THAN,
//                                                                                              Comparison.GREATER_THAN,
//                                                                                              Comparison.LESS_THAN_OR_EQUAL,
//                                                                                              Comparison.GREATER_THAN_OR_EQUAL,
//                                                                                              Comparison.BETWEEN), parameters);
//	}
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
////		if (this.inputValuesType1 == null) {
////			this.inputValuesType1 = new LinkedHashMap<String, ConditionletInput>();
////			ConditionletInput inputField1 = new ConditionletInput();
////			// Set field #1 configuration
////			inputField1.setId(INPUT1_ID);
////			inputField1.setUserInputAllowed(true);
////			inputField1.setMultipleSelectionAllowed(false);
////			inputField1.setMinNum(1);
////			this.inputValuesType1.put(inputField1.getId(), inputField1);
////		}
////		if (comparisonId.equalsIgnoreCase(COMPARISON_BETWEEN)) {
////			if (this.inputValuesType2 == null) {
////				this.inputValuesType2 = new LinkedHashMap<String, ConditionletInput>();
////				this.inputValuesType2.put(INPUT1_ID,
////						inputValuesType1.get(INPUT1_ID));
////				// Set field #2 configuration
////				ConditionletInput inputField2 = new ConditionletInput();
////				inputField2.setId(INPUT2_ID);
////				inputField2.setUserInputAllowed(true);
////				inputField2.setMultipleSelectionAllowed(false);
////				inputField2.setMinNum(1);
////				this.inputValuesType2.put(inputField2.getId(), inputField2);
////			}
////			return this.inputValuesType2.values();
////		}
//		return Lists.newArrayList();
//    }
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
//
////        long clientDateTime = 0;
////		try {
////			InetAddress address = HttpRequestDataUtil.getIpAddress(request);
////			String ipAddress = address.getHostAddress();
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
////		Map<String, String> conditionletValues = new HashMap<String, String>();
////		String inputValue1 = values.get(0).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT1_ID, inputValue1));
////		conditionletValues.put(INPUT1_ID, inputValue1);
////		if (comparison == Comparison.BETWEEN) {
////			String inputValue2 = values.get(1).getValue();
////			inputValues.add(new ConditionletInputValue(INPUT2_ID, inputValue2));
////			conditionletValues.put(INPUT2_ID, inputValue2);
////		}
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		return compareTime(comparison, clientDateTime, conditionletValues);
//        return false;
//	}
//
//	/**
//	 * Evaluates the client's time and the time specified in the conditionlet
//	 * using the defined comparison method.
//	 *
//	 * @param comparison
//	 *            - The {@link Comparison} method.
//	 * @param clientDateTime
//	 *            - The client's time.
//	 * @param conditionletValues
//	 *            - The {@link Map} containing the time specified in the
//	 *            conditionlet. If the comparison evaluates a range of values
//	 *            (such as "Between"), 2 times will be present.
//	 * @return If the client's time matches the specified comparison, returns
//	 *         {@code true}. Otherwise, returns {@code false}.
//	 */
//	private boolean compareTime(Comparison comparison, long clientDateTime, Map<String, String> conditionletValues) {
////		int clientTime = getTimeFromDate(clientDateTime);
////		int conditionletTime1 = getTimeFromDate(Long.valueOf(conditionletValues
////				.get(INPUT1_ID)));
////		if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
////			if (clientTime > conditionletTime1) {
////				return true;
////			}
////		} else if (comparison.getId().equals(
////				COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
////			if (clientTime >= conditionletTime1) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_EQUAL_TO)) {
////			if (clientTime == conditionletTime1) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
////			if (clientTime <= conditionletTime1) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN)) {
////			if (clientTime <= conditionletTime1) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_BETWEEN)) {
////			int conditionletTime2 = getTimeFromDate(Long
////					.valueOf(conditionletValues.get(INPUT2_ID)));
////			if (clientTime >= conditionletTime1
////					&& clientTime <= conditionletTime2) {
////				return true;
////			}
////		}
//		return false;
//	}
//
//	/**
//	 * Returns the appended hour and minutes of a given date as an integer so
//	 * that it can be compared with other times. The value of the hour will be
//	 * extracted (using the 24-hour format) and appended to the value of
//	 * minutes. This will generate a simple integer number that can be easily
//	 * compared with some other time (the date part is ignored).
//	 *
//	 * @param dateInMillis
//	 *            - The date in milliseconds where the time will be extracted.
//	 * @return The integer representation of the date's time. For example, if
//	 *         the time to process is "9:30am", the result will be {@code 930};
//	 *         for "4:15pm" the result will be {@code 1615}.
//	 */
//	private int getTimeFromDate(long dateInMillis) {
//		Calendar date = Calendar.getInstance();
//		date.setTimeInMillis(dateInMillis);
//		int hour = date.get(Calendar.HOUR_OF_DAY);
//		int minute = date.get(Calendar.MINUTE);
//		String timeAsStr = "" + hour + (minute < 10 ? "0" + minute : minute);
//		return Integer.parseInt(timeAsStr);
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
