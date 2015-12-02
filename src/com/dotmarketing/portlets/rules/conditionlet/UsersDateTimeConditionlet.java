package com.dotmarketing.portlets.rules.conditionlet;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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
 * This conditionlet will allow dotCMS users to check the client's current date
 * and time when a page was requested. This {@link Conditionlet} provides a
 * drop-down menu with the available comparison mechanisms, and a text field to
 * enter the date and time to compare. This date/time parameter will be
 * expressed in milliseconds in order to avoid any format-related and time zone
 * issues.
 * <p>
 * The date and time of the request is determined by the IP address of the
 * client that issued the request. Geographic information is then retrieved via
 * the <a href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java
 * API</a>.
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-07-2015
 *
 */
public class UsersDateTimeConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "datetime";
	private static final String CONDITIONLET_NAME = "User's Date & Time";

	private static final String COMPARISON_GREATER_THAN = "greater";
	private static final String COMPARISON_GREATER_THAN_OR_EQUAL_TO = "greaterOrEqual";
	private static final String COMPARISON_EQUAL_TO = "equal";
	private static final String COMPARISON_LOWER_THAN_OR_EQUAL_TO = "lowerOrEqual";
	private static final String COMPARISON_LOWER_THAN = "lower";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	public UsersDateTimeConditionlet() {
		super(CONDITIONLET_NAME);
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
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		if (!UtilMethods.isSet(values) || values.size() == 0
				|| !UtilMethods.isSet(comparisonId)) {
			return false;
		}
		long clientDateTime = 0;
		try {
			InetAddress ip = HttpRequestDataUtil.getIpAddress(request);
			String ipAddress = ip.getHostAddress();
			// TODO: Remove
			ipAddress = "170.123.234.133";
			Calendar date = GeoIp2CityDbUtil.getInstance().getDateTime(
					ipAddress);
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
		String inputValue = values.get(0).getValue();
		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		long conditionletInput = Long.parseLong(inputValue);
		if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
			if (clientDateTime > conditionletInput) {
				return true;
			}
		}
		if (comparison.getId().equals(COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
			if (clientDateTime >= conditionletInput) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_EQUAL_TO)) {
			if (clientDateTime == conditionletInput) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
			if (clientDateTime <= conditionletInput) {
				return true;
			}
		} else if (comparison.getId().equals(COMPARISON_LOWER_THAN)) {
			if (clientDateTime < conditionletInput) {
				return true;
			}
		}
		return false;
	}

}
