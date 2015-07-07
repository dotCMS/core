package com.dotmarketing.portlets.rules.conditionlet;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the city a user request comes
 * from. The comparison of city names is case-insensitive, except for the
 * regular expression comparison. This {@link Conditionlet} provides a drop-down
 * menu with the available comparison mechanisms, and a drop-down menu with the
 * capital cities of the states in the USA, where users can select one or more
 * values that will match the selected criterion.
 * <p>
 * The location of the request is determined by the IP address of the client
 * that issued the request. Geographic information is then retrieved via the <a
 * href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java API</a>.
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-16-2015
 *
 */
public class UsersCityConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "city";
	private static final String CONDITIONLET_NAME = "User's City (USA)";

	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	public UsersCityConditionlet() {
		super(CONDITIONLET_NAME);
	}

	@Override
	public Set<Comparison> getComparisons() {
		if (this.comparisons == null) {
			this.comparisons = new LinkedHashSet<Comparison>();
			this.comparisons.add(new Comparison(COMPARISON_IS, "Is"));
			this.comparisons.add(new Comparison(COMPARISON_ISNOT, "Is Not"));
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
			if (this.inputValues == null
					|| this.inputValues.get(inputId) == null) {
				getInputs(comparison.getId());
			}
			ConditionletInput inputField = this.inputValues.get(inputId);
			validationResult.setConditionletInputId(inputId);
			Set<EntryOption> inputOptions = inputField.getData();
			for (EntryOption option : inputOptions) {
				if (option.getId().equals(selectedValue)) {
					validationResult.setValid(true);
					break;
				}
			}
			if (!validationResult.isValid()) {
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
			// Set field configuration and available options
			inputField.setId(INPUT_ID);
			inputField.setMultipleSelectionAllowed(true);
			inputField.setDefaultValue("");
			inputField.setMinNum(1);
			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
			options.add(new EntryOption("Juneau", "Juneau"));
			options.add(new EntryOption("Montgomery", "Montgomery"));
			options.add(new EntryOption("Little Rock", "Little Rock"));
			options.add(new EntryOption("Phoenix", "Phoenix"));
			options.add(new EntryOption("Sacramento", "Sacramento"));
			options.add(new EntryOption("Denver", "Denver"));
			options.add(new EntryOption("Hartford", "Hartford"));
			options.add(new EntryOption("Dover", "Dover"));
			options.add(new EntryOption("Tallahassee", "Tallahassee"));
			options.add(new EntryOption("Atlanta", "Atlanta"));
			options.add(new EntryOption("Honolulu", "Honolulu"));
			options.add(new EntryOption("Des Moines", "Des Moines"));
			options.add(new EntryOption("Boise", "Boise"));
			options.add(new EntryOption("Springfield", "Springfield"));
			options.add(new EntryOption("Indianapolis", "Indianapolis"));
			options.add(new EntryOption("Topeka", "Topeka"));
			options.add(new EntryOption("Frankfort", "Frankfort"));
			options.add(new EntryOption("Baton Rouge", "Baton Rouge"));
			options.add(new EntryOption("Boston", "Boston"));
			options.add(new EntryOption("Annapolis", "Annapolis"));
			options.add(new EntryOption("Augusta", "Augusta"));
			options.add(new EntryOption("Lansing", "Lansing"));
			options.add(new EntryOption("Saint Paul", "Saint Paul"));
			options.add(new EntryOption("Jefferson City", "Jefferson City"));
			options.add(new EntryOption("Jackson", "Jackson"));
			options.add(new EntryOption("Helena", "Helena"));
			options.add(new EntryOption("Raleigh", "Raleigh"));
			options.add(new EntryOption("Bismark", "Bismark"));
			options.add(new EntryOption("Lincoln", "Lincoln"));
			options.add(new EntryOption("Concord", "Concord"));
			options.add(new EntryOption("Trenton", "Trenton"));
			options.add(new EntryOption("Santa Fe", "Santa Fe"));
			options.add(new EntryOption("Carson City", "Carson City"));
			options.add(new EntryOption("Albany", "Albany"));
			options.add(new EntryOption("Columbus", "Columbus"));
			options.add(new EntryOption("Oklahoma City", "Oklahoma City"));
			options.add(new EntryOption("Salem", "Salem"));
			options.add(new EntryOption("Harrisburg", "Harrisburg"));
			options.add(new EntryOption("Providence", "Providence"));
			options.add(new EntryOption("Columbia", "Columbia"));
			options.add(new EntryOption("Pierre", "Pierre"));
			options.add(new EntryOption("Nashville", "Nashville"));
			options.add(new EntryOption("Austin", "Austin"));
			options.add(new EntryOption("Salt Lake City", "Salt Lake City"));
			options.add(new EntryOption("Richmond", "Richmond"));
			options.add(new EntryOption("Montpelier", "Montpelier"));
			options.add(new EntryOption("Olympia", "Olympia"));
			options.add(new EntryOption("Madison", "Madison"));
			options.add(new EntryOption("Charleston", "Charleston"));
			options.add(new EntryOption("Cheyenne", "Cheyenne"));
			inputField.setData(options);
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
		GeoIp2CityDbUtil geoIp2Util = GeoIp2CityDbUtil.getInstance();
		String city = null;
		try {
			InetAddress address = HttpRequestDataUtil.getIpAddress(request);
			String ipAddress = address.getHostAddress();
			// TODO: Remove
			ipAddress = "170.123.234.133";
			city = geoIp2Util.getCityName(ipAddress);
		} catch (IOException | GeoIp2Exception e) {
			Logger.error(this,
					"An error occurred when retrieving the IP address from request: "
							+ request.getRequestURL());
		}
		if (!UtilMethods.isSet(city)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		for (ConditionValue value : values) {
			inputValues.add(new ConditionletInputValue(INPUT_ID, value
					.getValue()));
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		if (comparison.getId().equals(COMPARISON_IS)) {
			for (ConditionValue value : values) {
				if (value.getValue().equals(city)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
			for (ConditionValue value : values) {
				if (value.getValue().equals(city)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
