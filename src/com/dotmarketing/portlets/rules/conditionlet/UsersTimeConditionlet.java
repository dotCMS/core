package com.dotmarketing.portlets.rules.conditionlet;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the user's current time when
 * a page was requested.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-13-2015
 *
 */
public class UsersTimeConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT1_ID = "time1";
	private static final String INPUT2_ID = "time2";
	private static final String CONDITIONLET_NAME = "User's Time";

	private static final String COMPARISON_GREATER_THAN = "greater";
	private static final String COMPARISON_GREATER_THAN_OR_EQUAL_TO = "greaterOrEqual";
	private static final String COMPARISON_EQUAL_TO = "equal";
	private static final String COMPARISON_LOWER_THAN = "lower";
	private static final String COMPARISON_LOWER_THAN_OR_EQUAL_TO = "lowerOrEqual";
	private static final String COMPARISON_BETWEEN = "between";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValuesType1 = null;
	private Map<String, ConditionletInput> inputValuesType2 = null;

	@Override
	protected String getName() {
		return CONDITIONLET_NAME;
	}

	@Override
	public Set<Comparison> getComparisons() {
		if (this.comparisons == null) {
			this.comparisons = new LinkedHashSet<Comparison>();
			this.comparisons.add(new Comparison(COMPARISON_GREATER_THAN,
					"Is Greater Than"));
			this.comparisons.add(new Comparison(
					COMPARISON_GREATER_THAN_OR_EQUAL_TO,
					"Is Greater Than or Equal To"));
			this.comparisons.add(new Comparison(COMPARISON_EQUAL_TO,
					"Is Equal To"));
			this.comparisons.add(new Comparison(
					COMPARISON_LOWER_THAN_OR_EQUAL_TO,
					"Is Lower Than or Equal To"));
			this.comparisons.add(new Comparison(COMPARISON_LOWER_THAN,
					"Is Lower Than"));
			this.comparisons.add(new Comparison(COMPARISON_BETWEEN,
					"Is Between"));
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
			// Number of digits in long number ranges from 1 to 19
			if (Pattern.matches("\\d{1,19}", selectedValue)) {
				validationResult.setValid(true);
			} else {
				validationResult.setErrorMessage("Invalid value for input '"
						+ inputId + "': '" + selectedValue + "'");
			}
		}
		return validationResult;
	}

	@Override
	public Collection<ConditionletInput> getInputs(String comparisonId) {
		if (this.inputValuesType1 == null) {
			this.inputValuesType1 = new LinkedHashMap<String, ConditionletInput>();
			ConditionletInput inputField1 = new ConditionletInput();
			// Set field #1 configuration
			inputField1.setId(INPUT1_ID);
			inputField1.setUserInputAllowed(true);
			inputField1.setMultipleSelectionAllowed(false);
			inputField1.setMinNum(1);
			this.inputValuesType1.put(inputField1.getId(), inputField1);
		}
		if (comparisonId.equalsIgnoreCase(COMPARISON_BETWEEN)) {
			if (this.inputValuesType2 == null) {
				this.inputValuesType2 = new LinkedHashMap<String, ConditionletInput>();
				this.inputValuesType2.put(INPUT1_ID,
						inputValuesType1.get(INPUT1_ID));
				// Set field #2 configuration
				ConditionletInput inputField2 = new ConditionletInput();
				inputField2.setId(INPUT2_ID);
				inputField2.setUserInputAllowed(true);
				inputField2.setMultipleSelectionAllowed(false);
				inputField2.setMinNum(1);
				this.inputValuesType2.put(inputField2.getId(), inputField2);
			}
			return this.inputValuesType2.values();
		}
		return this.inputValuesType1.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		if (!UtilMethods.isSet(values) || values.size() > 0
				&& !UtilMethods.isSet(comparisonId)) {
			return false;
		}
		long clientDateTime = 0;
		try {
			InetAddress ipAddress = HttpRequestDataUtil.getIpAddress(request);
			String ip = ipAddress.getHostAddress();
			Calendar date = GeoIp2CityDbUtil.getInstance().getDateTime(ip);
			clientDateTime = date.getTime().getTime();
		} catch (IOException | GeoIp2Exception e) {
			Logger.error(
					this,
					"Could not retrieved a valid date from request: "
							+ request.getRequestURL());
		}
		if (clientDateTime == 0) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		Map<String, String> conditionletValues = new HashMap<String, String>();
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(value.getId(), value
					.getValue()));
			conditionletValues.put(value.getId(), value.getValue());
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		return compareTime(comparison, clientDateTime, conditionletValues);
	}

	/**
	 * Evaluates the client's time and the time specified in the conditionlet
	 * using the defined comparison method.
	 * 
	 * @param comparison
	 *            - The {@link Comparison} method.
	 * @param clientDateTime
	 *            - The client's time.
	 * @param conditionletValues
	 *            - The {@link Map} containing the time specified in the
	 *            conditionlet. If the comparison evaluates a range of values
	 *            (such as "Between"), 2 times will be present.
	 * @return If the client's time matches the specified comparison, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean compareTime(Comparison comparison, long clientDateTime,
			Map<String, String> conditionletValues) {
		int clientTime = getTimeFromDate(clientDateTime);
		int conditionletTime1 = getTimeFromDate(Long.valueOf(conditionletValues
				.get(INPUT1_ID)));
		if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
			if (clientTime > conditionletTime1) {
				return true;
			}
		} else if (comparison.getId().equals(
				COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
			if (clientTime >= conditionletTime1) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_EQUAL_TO)) {
			if (clientTime == conditionletTime1) {
				return true;
			}
		} else if (comparison.getId().startsWith(
				COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
			if (clientTime <= conditionletTime1) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_LOWER_THAN)) {
			if (clientTime <= conditionletTime1) {
				return true;
			}
		} else if (comparison.getId().startsWith(COMPARISON_BETWEEN)) {
			int conditionletTime2 = getTimeFromDate(Long
					.valueOf(conditionletValues.get(INPUT2_ID)));
			if (clientTime >= conditionletTime1
					&& clientTime <= conditionletTime2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the appended hour and minutes of a given date as an integer so
	 * that it can be compared with other times. The value of the hour will be
	 * extracted (using the 24-hour format) and appended to the value of
	 * minutes. This will generate a simple integer number that can be easily
	 * compared with some other time.
	 * 
	 * @param dateInMillis
	 *            - The date in milliseconds where the time will be extracted.
	 * @return The integer representation of the date's time. For example, if
	 *         the time to process is "9:30am", the result will be {@code 930};
	 *         for "4:15pm" the result will be {@code 1615}.
	 */
	private int getTimeFromDate(long dateInMillis) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(dateInMillis);
		int hour = date.get(Calendar.HOUR_OF_DAY);
		int minute = date.get(Calendar.MINUTE);
		String timeAsStr = "" + hour + (minute < 10 ? "0" + minute : minute);
		return Integer.parseInt(timeAsStr);
	}

}
