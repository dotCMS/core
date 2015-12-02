package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
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
 * This conditionlet will allow CMS users to check the country a user request
 * comes from. The available options of this conditionlet will be represented as
 * two-character values. This {@link Conditionlet} provides a drop-down menu
 * with the available comparison mechanisms, and a drop-down menu with the
 * countries of the world, where users can select one or more values that will
 * match the selected criterion.
 * <p>
 * The location of the request is determined by the IP address of the client
 * that issued the request. Geographic information is then retrieved via the <a
 * href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java API</a>.
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-17-2015
 *
 */
public class UsersCountryConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

    private static final String I18N_BASE = "api.system.ruleengine.conditionlet.VisitorCountry";

    private static final String INPUT_ID = "country";

	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";

    private static final String ISO_CODE = "isoCode";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;
    private final GeoIp2CityDbUtil geoIp2Util;

    public UsersCountryConditionlet() {
		this(GeoIp2CityDbUtil.getInstance());
	}

    @VisibleForTesting
    protected UsersCountryConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super(I18N_BASE);
        this.geoIp2Util = geoIp2Util;
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
			inputField.setDefaultValue("AL");
			inputField.setMinNum(1);
			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
			options.add(new EntryOption("AF", "Afganistan"));
			options.add(new EntryOption("AL", "Albania"));
			options.add(new EntryOption("DZ", "Algeria"));
			options.add(new EntryOption("AS", "American Samoa"));
			options.add(new EntryOption("AD", "Andorra"));
			options.add(new EntryOption("AO", "Angola"));
			options.add(new EntryOption("AI", "Anguilla"));
			options.add(new EntryOption("AQ", "Antarctica"));
			options.add(new EntryOption("AR", "Argentina"));
			options.add(new EntryOption("AM", "Armenia"));
			options.add(new EntryOption("AW", "Aruba"));
			options.add(new EntryOption("AU", "Australia"));
			options.add(new EntryOption("AT", "Austria"));
			options.add(new EntryOption("AZ", "Azerbaijan"));
			options.add(new EntryOption("BS", "Bahamas"));
			options.add(new EntryOption("BH", "Bahrain"));
			options.add(new EntryOption("BD", "Bangladesh"));
			options.add(new EntryOption("BB", "Barbados"));
			options.add(new EntryOption("BY", "Belarus"));
			options.add(new EntryOption("BE", "Belgium"));
			options.add(new EntryOption("BZ", "Belize"));
			options.add(new EntryOption("BJ", "Benin"));
			options.add(new EntryOption("BM", "Bermuda"));
			options.add(new EntryOption("BT", "Bhutan"));
			options.add(new EntryOption("BO", "Bolivia"));
			options.add(new EntryOption("BA", "Bosnia and Herzegovina"));
			options.add(new EntryOption("BW", "Botswana"));
			options.add(new EntryOption("BR", "Brazil"));
			options.add(new EntryOption("VG", "British Virgin Islands"));
			options.add(new EntryOption("BN", "Brunei"));
			options.add(new EntryOption("BG", "Bulgaria"));
			options.add(new EntryOption("BF", "Burkina Faso"));
			options.add(new EntryOption("BI", "Burundi"));
			options.add(new EntryOption("KH", "Cambodia"));
			options.add(new EntryOption("CM", "Cameroon"));
			options.add(new EntryOption("CA", "Canada"));
			options.add(new EntryOption("CV", "Cape Verde"));
			options.add(new EntryOption("KY", "Cayman Islands"));
			options.add(new EntryOption("CF", "Central African Republic"));
			options.add(new EntryOption("CL", "Chile"));
			options.add(new EntryOption("CN", "China"));
			options.add(new EntryOption("CO", "Colombia"));
			options.add(new EntryOption("KM", "Comoros"));
			options.add(new EntryOption("CK", "Cook Islands"));
			options.add(new EntryOption("CR", "Costa Rica"));
			options.add(new EntryOption("HR", "Croatia"));
			options.add(new EntryOption("CU", "Cuba"));
			options.add(new EntryOption("CW", "Curacao"));
			options.add(new EntryOption("CY", "Cyprus"));
			options.add(new EntryOption("CZ", "Czech Republic"));
			options.add(new EntryOption("CD", "Democratic Republic of Congo"));
			options.add(new EntryOption("DK", "Denmark"));
			options.add(new EntryOption("DJ", "Djibouti"));
			options.add(new EntryOption("DM", "Dominica"));
			options.add(new EntryOption("DO", "Dominican Republic"));
			options.add(new EntryOption("TL", "East Timor"));
			options.add(new EntryOption("EC", "Ecuador"));
			options.add(new EntryOption("EG", "Egypt"));
			options.add(new EntryOption("SV", "El Salvador"));
			options.add(new EntryOption("GQ", "Equatorial Guinea"));
			options.add(new EntryOption("ER", "Eritrea"));
			options.add(new EntryOption("EE", "Estonia"));
			options.add(new EntryOption("ET", "Ethiopia"));
			options.add(new EntryOption("FK", "Falkland Islands"));
			options.add(new EntryOption("FO", "Faroe Islands"));
			options.add(new EntryOption("FJ", "Fiji"));
			options.add(new EntryOption("FI", "Finland"));
			options.add(new EntryOption("FR", "France"));
			options.add(new EntryOption("PF", "French Polynesia"));
			options.add(new EntryOption("GA", "Gabon"));
			options.add(new EntryOption("GM", "Gambia"));
			options.add(new EntryOption("GE", "Georgia"));
			options.add(new EntryOption("DE", "Germany"));
			options.add(new EntryOption("GH", "Ghana"));
			options.add(new EntryOption("GI", "Gibraltar"));
			options.add(new EntryOption("GR", "Greece"));
			options.add(new EntryOption("GL", "Greenland"));
			options.add(new EntryOption("GP", "Guadeloupe"));
			options.add(new EntryOption("GU", "Guam"));
			options.add(new EntryOption("GT", "Guatemala"));
			options.add(new EntryOption("GN", "Guinea"));
			options.add(new EntryOption("GW", "Guinea-Bissau"));
			options.add(new EntryOption("GY", "Guyana"));
			options.add(new EntryOption("HT", "Haiti"));
			options.add(new EntryOption("HN", "Honduras"));
			options.add(new EntryOption("HK", "Hong Kong"));
			options.add(new EntryOption("HU", "Hungary"));
			options.add(new EntryOption("IS", "Iceland"));
			options.add(new EntryOption("IN", "India"));
			options.add(new EntryOption("ID", "Indonesia"));
			options.add(new EntryOption("IR", "Iran"));
			options.add(new EntryOption("IQ", "Irak"));
			options.add(new EntryOption("IE", "Ireland"));
			options.add(new EntryOption("IM", "Isle of Man"));
			options.add(new EntryOption("IL", "Israel"));
			options.add(new EntryOption("IT", "Italy"));
			options.add(new EntryOption("CI", "Ivory Coast"));
			options.add(new EntryOption("JM", "Jamaica"));
			options.add(new EntryOption("JP", "Japan"));
			options.add(new EntryOption("JO", "Jordan"));
			options.add(new EntryOption("KZ", "Kazakhstan"));
			options.add(new EntryOption("KE", "Kenya"));
			options.add(new EntryOption("KI", "Kiribati"));
			options.add(new EntryOption("XK", "Kosovo"));
			options.add(new EntryOption("KW", "Kuwait"));
			options.add(new EntryOption("KG", "Kyrgyzstan"));
			options.add(new EntryOption("LA", "Laos"));
			options.add(new EntryOption("LV", "Latvia"));
			options.add(new EntryOption("LB", "Lebanon"));
			options.add(new EntryOption("LS", "Lesotho"));
			options.add(new EntryOption("LR", "Liberia"));
			options.add(new EntryOption("LY", "Libya"));
			options.add(new EntryOption("LI", "Liechtenstein"));
			options.add(new EntryOption("LT", "Lithuania"));
			options.add(new EntryOption("LU", "Luxembourg"));
			options.add(new EntryOption("MO", "Macau"));
			options.add(new EntryOption("MK", "Macedonia"));
			options.add(new EntryOption("MG", "Madagascar"));
			options.add(new EntryOption("MW", "Malawi"));
			options.add(new EntryOption("MY", "Malaysia"));
			options.add(new EntryOption("MV", "Maldives"));
			options.add(new EntryOption("ML", "Mali"));
			options.add(new EntryOption("MT", "Malta"));
			options.add(new EntryOption("MH", "Marshall Islands"));
			options.add(new EntryOption("MR", "Mauritania"));
			options.add(new EntryOption("MU", "Mauritius"));
			options.add(new EntryOption("MX", "Mexico"));
			options.add(new EntryOption("FM", "Micronesia"));
			options.add(new EntryOption("MD", "Moldova"));
			options.add(new EntryOption("MC", "Monaco"));
			options.add(new EntryOption("MN", "Mongolia"));
			options.add(new EntryOption("ME", "Montenegro"));
			options.add(new EntryOption("MS", "Montserrat"));
			options.add(new EntryOption("MA", "Morocco"));
			options.add(new EntryOption("MZ", "Mozambique"));
			options.add(new EntryOption("MM", "Myanmar"));
			options.add(new EntryOption("NA", "Namibia"));
			options.add(new EntryOption("NR", "Nauru"));
			options.add(new EntryOption("NP", "Nepal"));
			options.add(new EntryOption("NL", "Netherlands"));
			options.add(new EntryOption("NC", "New Caledonia"));
			options.add(new EntryOption("NZ", "New Zealand"));
			options.add(new EntryOption("NI", "Nicaragua"));
			options.add(new EntryOption("NE", "Niger"));
			options.add(new EntryOption("NG", "Nigeria"));
			options.add(new EntryOption("NU", "Niue"));
			options.add(new EntryOption("NF", "Norfolk Island"));
			options.add(new EntryOption("KP", "North Korea"));
			options.add(new EntryOption("MP", "Northern Mariana Islands"));
			options.add(new EntryOption("NO", "Norway"));
			options.add(new EntryOption("OM", "Oman"));
			options.add(new EntryOption("PK", "Pakistan"));
			options.add(new EntryOption("PW", "Palau"));
			options.add(new EntryOption("PA", "Panama"));
			options.add(new EntryOption("PG", "Papua New Guinea"));
			options.add(new EntryOption("PY", "Paraguay"));
			options.add(new EntryOption("PE", "Peru"));
			options.add(new EntryOption("PH", "Philippines"));
			options.add(new EntryOption("PN", "Pitcairn Islands"));
			options.add(new EntryOption("PL", "Poland"));
			options.add(new EntryOption("PT", "Portugal"));
			options.add(new EntryOption("PR", "Puerto Rico"));
			options.add(new EntryOption("QA", "Qatar"));
			options.add(new EntryOption("CG", "Republic of the Congo"));
			options.add(new EntryOption("RE", "Reunion"));
			options.add(new EntryOption("RO", "Romania"));
			options.add(new EntryOption("RU", "Russia"));
			options.add(new EntryOption("RW", "Rwanda"));
			options.add(new EntryOption("BL", "Saint Barthelemy"));
			options.add(new EntryOption("SH", "Saint Helena"));
			options.add(new EntryOption("KN", "Saint Kitts and Nevis"));
			options.add(new EntryOption("LC", "Saint Lucia"));
			options.add(new EntryOption("MF", "Saint Martin"));
			options.add(new EntryOption("PM", "Saint Pierre and Miquelon"));
			options.add(new EntryOption("VC",
					"Saint Vincent and the Grenadines"));
			options.add(new EntryOption("WS", "Samoa"));
			options.add(new EntryOption("SM", "San Marino"));
			options.add(new EntryOption("ST", "Sao Tome and Principe"));
			options.add(new EntryOption("SA", "Saudi Arabia"));
			options.add(new EntryOption("SN", "Senegal"));
			options.add(new EntryOption("RS", "Serbia"));
			options.add(new EntryOption("SC", "Seychelles"));
			options.add(new EntryOption("SL", "Sierra Leone"));
			options.add(new EntryOption("SG", "Singapore"));
			options.add(new EntryOption("SK", "Slovakia"));
			options.add(new EntryOption("SI", "Slovenia"));
			options.add(new EntryOption("SB", "Solomon Islands"));
			options.add(new EntryOption("SO", "Somalia"));
			options.add(new EntryOption("ZA", "South Africa"));
			options.add(new EntryOption("KR", "South Korea"));
			options.add(new EntryOption("SS", "South Sudan"));
			options.add(new EntryOption("ES", "Spain"));
			options.add(new EntryOption("LK", "Sri Lanka"));
			options.add(new EntryOption("SD", "Sudan"));
			options.add(new EntryOption("SR", "Suriname"));
			options.add(new EntryOption("SZ", "Swaziland"));
			options.add(new EntryOption("SE", "Sweeden"));
			options.add(new EntryOption("CH", "Switzerland"));
			options.add(new EntryOption("SY", "Syria"));
			options.add(new EntryOption("TW", "Taiwan"));
			options.add(new EntryOption("TJ", "Tajikistan"));
			options.add(new EntryOption("TZ", "Tanzania"));
			options.add(new EntryOption("TH", "Thailand"));
			options.add(new EntryOption("TG", "Togo"));
			options.add(new EntryOption("TK", "Tokelau"));
			options.add(new EntryOption("TT", "Trinidad and Tobago"));
			options.add(new EntryOption("TN", "Tunisia"));
			options.add(new EntryOption("TR", "Turkey"));
			options.add(new EntryOption("TM", "Turkmenistan"));
			options.add(new EntryOption("TV", "Tuvalu"));
			options.add(new EntryOption("UG", "Uganda"));
			options.add(new EntryOption("UA", "Ukraine"));
			options.add(new EntryOption("AE", "United Arab Emirates"));
			options.add(new EntryOption("GB", "United Kingdom"));
			options.add(new EntryOption("US", "United States"));
			options.add(new EntryOption("UY", "Uruguay"));
			options.add(new EntryOption("UZ", "Uzbekistan"));
			options.add(new EntryOption("VU", "Vanuatu"));
			options.add(new EntryOption("VA", "Vatican"));
			options.add(new EntryOption("VE", "Venezuela"));
			options.add(new EntryOption("VN", "Vietnam"));
			options.add(new EntryOption("EH", "Western Sahara"));
			options.add(new EntryOption("YE", "Yemen"));
			options.add(new EntryOption("ZM", "Zambia"));
			options.add(new EntryOption("ZW", "Zimbabwe"));
			inputField.setData(options);
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			this.inputValues.put(inputField.getId(), inputField);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
                            HttpServletResponse response,
                            String comparisonId,
                            List<ConditionValue> values) {
		if (!UtilMethods.isSet(values) || values.size() == 0 || !UtilMethods.isSet(comparisonId)) {
			return false;
		}
        String country = null;
		try {
			InetAddress address = HttpRequestDataUtil.getIpAddress(request);
			String ipAddress = address.getHostAddress();
			country = geoIp2Util.getCountryIsoCode(ipAddress);
		} catch (IOException | GeoIp2Exception e) {
			Logger.error(this,
					"An error occurred when retrieving the IP address from request: "
							+ request.getRequestURL());
		}
		if (!UtilMethods.isSet(country)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
		for (ConditionValue value : values) {
			if(value.getKey().equals(ISO_CODE))
                inputValues.add(new ConditionletInputValue(INPUT_ID, value
					.getValue()));
		}
		ValidationResults validationResults = validate(comparison, inputValues);
		if (validationResults.hasErrors()) {
			return false;
		}
		if (comparison.getId().equals(COMPARISON_IS)) {
			for (ConditionValue value : values) {
				if (value.getValue().equalsIgnoreCase(country)) {
					return true;
				}
			}
		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
			for (ConditionValue value : values) {
				if (value.getValue().equalsIgnoreCase(country)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
