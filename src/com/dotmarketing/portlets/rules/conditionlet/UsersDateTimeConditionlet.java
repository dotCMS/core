package com.dotmarketing.portlets.rules.conditionlet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
 * This conditionlet will allow CMS users to check the user's current date and
 * time when a page was requested.
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
	private static final String COMPARISON_LOWER_THAN = "lower";
	private static final String COMPARISON_LOWER_THAN_OR_EQUAL_TO = "lowerOrEqual";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

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
			this.comparisons.add(new Comparison(COMPARISON_LOWER_THAN,
					"Is Lower Than"));
			this.comparisons.add(new Comparison(
					COMPARISON_LOWER_THAN_OR_EQUAL_TO,
					"Is Lower Than or Equal To"));
		}
		return this.comparisons;
	}

	@Override
	public ValidationResults validate(Comparison comparison,
			Set<ConditionletInputValue> inputValues) {
		ValidationResults results = new ValidationResults();
		if (UtilMethods.isSet(inputValues)) {
			List<ValidationResult> resultList = new ArrayList<ValidationResult>();
			// Validate all available input fields
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
						+ INPUT_ID + "': '" + selectedValue + "'");
			}
		}
		return validationResult;
	}

	@Override
	public Collection<ConditionletInput> getInputs(String comparisonId) {
		if (this.inputValues == null) {
			ConditionletInput inputField = new ConditionletInput();
			// Set field configuration
			inputField.setId(INPUT_ID);
			inputField.setUserInputAllowed(true);
			inputField.setMultipleSelectionAllowed(false);
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			this.inputValues.put(inputField.getId(), inputField);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		boolean result = false;
		if (UtilMethods.isSet(values) && values.size() > 0
				&& UtilMethods.isSet(comparisonId)) {
			long clientDateTime = 0;
			InetAddress ipAddress = null;
			String ip = null;
			try {
				ipAddress = HttpRequestDataUtil.getIpAddress(request);
				ip = ipAddress.getHostAddress();
				// TODO Remove later
				ip = "54.209.28.36";
				Date date = GeoIp2CityDbUtil.getInstance().getDateTime(ip);
				clientDateTime = date.getTime();
			} catch (UnknownHostException e) {
				Logger.error(this,
						"Could not retrieved a valid date from request: "
								+ request.getRequestURL());
			} catch (IOException e) {
				Logger.error(this,
						"Could not establish connection to GeoIP2 database for IP "
								+ ip);
			} catch (GeoIp2Exception e) {
				Logger.error(this,
						"Current date could not be retreived for IP " + ip);
			}
			if (clientDateTime == 0) {
				return result;
			}
			Comparison comparison = getComparisonById(comparisonId);
			Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
			String inputValue = null;
			for (ConditionValue value : values) {
				inputValues.add(new ConditionletInputValue(INPUT_ID, value
						.getValue()));
				inputValue = value.getValue();
			}
			ValidationResults validationResults = validate(comparison,
					inputValues);
			if (!validationResults.hasErrors()) {
				long conditionletInput = Long.parseLong(inputValue);
				if (comparison.getId().equals(COMPARISON_GREATER_THAN)) {
					if (clientDateTime > conditionletInput) {
						result = true;
					}
				}
				if (comparison.getId().equals(
						COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
					if (clientDateTime >= conditionletInput) {
						result = true;
					}
				} else if (comparison.getId().startsWith(COMPARISON_EQUAL_TO)) {
					if (clientDateTime == conditionletInput) {
						result = true;
					}
				} else if (comparison.getId().startsWith(
						COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
					if (clientDateTime <= conditionletInput) {
						result = true;
					}
				} else if (comparison.getId().startsWith(COMPARISON_LOWER_THAN)) {
					if (clientDateTime < conditionletInput) {
						result = true;
					}
				}
			}
		}
		return result;
	}

}
