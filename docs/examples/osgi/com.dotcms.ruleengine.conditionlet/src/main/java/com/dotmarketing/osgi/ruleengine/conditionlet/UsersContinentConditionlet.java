package com.dotmarketing.osgi.ruleengine.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.conditionlet.EntryOption;
import com.dotmarketing.portlets.rules.conditionlet.ValidationResult;
import com.dotmarketing.portlets.rules.conditionlet.ValidationResults;
import com.dotmarketing.portlets.rules.conditionlet.ConditionletInputValue;
import com.dotmarketing.portlets.rules.conditionlet.Comparison;
import com.dotmarketing.portlets.rules.conditionlet.ConditionletInput;

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
 * This conditionlet will allow CMS users to check the continent a user request
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
 * @author Oscar Arrieta
 * @version 1.0
 * @since 10-16-2015
 *
 */
public class UsersContinentConditionlet extends Conditionlet {

    private static final long serialVersionUID = 1L;

    private static final String INPUT_ID = "country";

    private static final String I18N_BASE = "com.dotmarketing.osgi.ruleengine.conditionlet";
    private static final String CONDITIONLET_COUNTRY_AFRICA = "com.dotmarketing.osgi.ruleengine.conditionlet.country.africa";
    private static final String CONDITIONLET_COUNTRY_ANTARCTICA = "com.dotmarketing.osgi.ruleengine.conditionlet.country.antarctica";
    private static final String CONDITIONLET_COUNTRY_ASIA = "com.dotmarketing.osgi.ruleengine.conditionlet.country.asia";
    private static final String CONDITIONLET_COUNTRY_EUROPE = "com.dotmarketing.osgi.ruleengine.conditionlet.country.europe";
    private static final String CONDITIONLET_COUNTRY_NORTH_AMERICA = "com.dotmarketing.osgi.ruleengine.conditionlet.country.northamerica";
    private static final String CONDITIONLET_COUNTRY_OCEANIA = "com.dotmarketing.osgi.ruleengine.conditionlet.country.oceania";
    private static final String CONDITIONLET_COUNTRY_SOUTH_AMERICA = "com.dotmarketing.osgi.ruleengine.conditionlet.country.southamerica";

    private static final String COMPARISON_IS = "is";
    private static final String COMPARISON_ISNOT = "isNot";

    private static final String ISO_CODE = "isoCode";

    private LinkedHashSet<Comparison> comparisons = null;
    private Map<String, ConditionletInput> inputValues = null;
    private final GeoIp2CityDbUtil geoIp2Util;

    public UsersContinentConditionlet() {
        this(GeoIp2CityDbUtil.getInstance());
    }

    @VisibleForTesting
    protected UsersContinentConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super(I18N_BASE);
        this.geoIp2Util = geoIp2Util;
    }

    @Override
    public Set<Comparison> getComparisons() {
        if (this.comparisons == null) {
            this.comparisons = new LinkedHashSet<Comparison>();
            this.comparisons.add(new Comparison(COMPARISON_IS));
            this.comparisons.add(new Comparison(COMPARISON_ISNOT));
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
            inputField.setDefaultValue("NA");
            inputField.setMinNum(1);
            Set<EntryOption> options = new LinkedHashSet<EntryOption>();
            options.add(new EntryOption("AF", CONDITIONLET_COUNTRY_AFRICA));
            options.add(new EntryOption("AN", CONDITIONLET_COUNTRY_ANTARCTICA));
            options.add(new EntryOption("AS", CONDITIONLET_COUNTRY_ASIA));
            options.add(new EntryOption("EU", CONDITIONLET_COUNTRY_EUROPE));
            options.add(new EntryOption("NA", CONDITIONLET_COUNTRY_NORTH_AMERICA));
            options.add(new EntryOption("OC", CONDITIONLET_COUNTRY_OCEANIA));
            options.add(new EntryOption("SA", CONDITIONLET_COUNTRY_SOUTH_AMERICA));
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
        String continent = continentCountryMap.get(country);
        if (!UtilMethods.isSet(continent)) {
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
                if (value.getValue().equalsIgnoreCase(continent)) {
                    return true;
                }
            }
        } else if (comparison.getId().equals(COMPARISON_ISNOT)) {
            for (ConditionValue value : values) {
                if (value.getValue().equalsIgnoreCase(continent)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static final Map<String, String> continentCountryMap = ImmutableMap.<String, String>builder()
        .put("AD", "EU")
        .put("AE","AS")
        .put("AF","AS")
        .put("AG","NA")
        .put("AI","NA")
        .put("AL","EU")
        .put("AM","AS")
        .put("AN","NA")
        .put("AO","AF")
        .put("AP","AS")
        .put("AQ","AN")
        .put("AR","SA")
        .put("AS","OC")
        .put("AT","EU")
        .put("AU","OC")
        .put("AW","NA")
        .put("AX","EU")
        .put("AZ","AS")
        .put("BA","EU")
        .put("BB","NA")
        .put("BD","AS")
        .put("BE","EU")
        .put("BF","AF")
        .put("BG","EU")
        .put("BH","AS")
        .put("BI","AF")
        .put("BJ","AF")
        .put("BL","NA")
        .put("BM","NA")
        .put("BN","AS")
        .put("BO","SA")
        .put("BR","SA")
        .put("BS","NA")
        .put("BT","AS")
        .put("BV","AN")
        .put("BW","AF")
        .put("BY","EU")
        .put("BZ","NA")
        .put("CA","NA")
        .put("CC","AS")
        .put("CD","AF")
        .put("CF","AF")
        .put("CG","AF")
        .put("CH","EU")
        .put("CI","AF")
        .put("CK","OC")
        .put("CL","SA")
        .put("CM","AF")
        .put("CN","AS")
        .put("CO","SA")
        .put("CR","NA")
        .put("CU","NA")
        .put("CV","AF")
        .put("CX","AS")
        .put("CY","AS")
        .put("CZ","EU")
        .put("DE","EU")
        .put("DJ","AF")
        .put("DK","EU")
        .put("DM","NA")
        .put("DO","NA")
        .put("DZ","AF")
        .put("EC","SA")
        .put("EE","EU")
        .put("EG","AF")
        .put("EH","AF")
        .put("ER","AF")
        .put("ES","EU")
        .put("ET","AF")
        .put("EU","EU")
        .put("FI","EU")
        .put("FJ","OC")
        .put("FK","SA")
        .put("FM","OC")
        .put("FO","EU")
        .put("FR","EU")
        .put("FX","EU")
        .put("GA","AF")
        .put("GB","EU")
        .put("GD","NA")
        .put("GE","AS")
        .put("GF","SA")
        .put("GG","EU")
        .put("GH","AF")
        .put("GI","EU")
        .put("GL","NA")
        .put("GM","AF")
        .put("GN","AF")
        .put("GP","NA")
        .put("GQ","AF")
        .put("GR","EU")
        .put("GS","AN")
        .put("GT","NA")
        .put("GU","OC")
        .put("GW","AF")
        .put("GY","SA")
        .put("HK","AS")
        .put("HM","AN")
        .put("HN","NA")
        .put("HR","EU")
        .put("HT","NA")
        .put("HU","EU")
        .put("ID","AS")
        .put("IE","EU")
        .put("IL","AS")
        .put("IM","EU")
        .put("IN","AS")
        .put("IO","AS")
        .put("IQ","AS")
        .put("IR","AS")
        .put("IS","EU")
        .put("IT","EU")
        .put("JE","EU")
        .put("JM","NA")
        .put("JO","AS")
        .put("JP","AS")
        .put("KE","AF")
        .put("KG","AS")
        .put("KH","AS")
        .put("KI","OC")
        .put("KM","AF")
        .put("KN","NA")
        .put("KP","AS")
        .put("KR","AS")
        .put("KW","AS")
        .put("KY","NA")
        .put("KZ","AS")
        .put("LA","AS")
        .put("LB","AS")
        .put("LC","NA")
        .put("LI","EU")
        .put("LK","AS")
        .put("LR","AF")
        .put("LS","AF")
        .put("LT","EU")
        .put("LU","EU")
        .put("LV","EU")
        .put("LY","AF")
        .put("MA","AF")
        .put("MC","EU")
        .put("MD","EU")
        .put("ME","EU")
        .put("MF","NA")
        .put("MG","AF")
        .put("MH","OC")
        .put("MK","EU")
        .put("ML","AF")
        .put("MM","AS")
        .put("MN","AS")
        .put("MO","AS")
        .put("MP","OC")
        .put("MQ","NA")
        .put("MR","AF")
        .put("MS","NA")
        .put("MT","EU")
        .put("MU","AF")
        .put("MV","AS")
        .put("MW","AF")
        .put("MX","NA")
        .put("MY","AS")
        .put("MZ","AF")
        .put("NA","AF")
        .put("NC","OC")
        .put("NE","AF")
        .put("NF","OC")
        .put("NG","AF")
        .put("NI","NA")
        .put("NL","EU")
        .put("NO","EU")
        .put("NP","AS")
        .put("NR","OC")
        .put("NU","OC")
        .put("NZ","OC")
        .put("OM","AS")
        .put("PA","NA")
        .put("PE","SA")
        .put("PF","OC")
        .put("PG","OC")
        .put("PH","AS")
        .put("PK","AS")
        .put("PL","EU")
        .put("PM","NA")
        .put("PN","OC")
        .put("PR","NA")
        .put("PS","AS")
        .put("PT","EU")
        .put("PW","OC")
        .put("PY","SA")
        .put("QA","AS")
        .put("RE","AF")
        .put("RO","EU")
        .put("RS","EU")
        .put("RU","EU")
        .put("RW","AF")
        .put("SA","AS")
        .put("SB","OC")
        .put("SC","AF")
        .put("SD","AF")
        .put("SE","EU")
        .put("SG","AS")
        .put("SH","AF")
        .put("SI","EU")
        .put("SJ","EU")
        .put("SK","EU")
        .put("SL","AF")
        .put("SM","EU")
        .put("SN","AF")
        .put("SO","AF")
        .put("SR","SA")
        .put("ST","AF")
        .put("SV","NA")
        .put("SY","AS")
        .put("SZ","AF")
        .put("TC","NA")
        .put("TD","AF")
        .put("TF","AN")
        .put("TG","AF")
        .put("TH","AS")
        .put("TJ","AS")
        .put("TK","OC")
        .put("TL","AS")
        .put("TM","AS")
        .put("TN","AF")
        .put("TO","OC")
        .put("TR","EU")
        .put("TT","NA")
        .put("TV","OC")
        .put("TW","AS")
        .put("TZ","AF")
        .put("UA","EU")
        .put("UG","AF")
        .put("UM","OC")
        .put("US","NA")
        .put("UY","SA")
        .put("UZ","AS")
        .put("VA","EU")
        .put("VC","NA")
        .put("VE","SA")
        .put("VG","NA")
        .put("VI","NA")
        .put("VN","AS")
        .put("VU","OC")
        .put("WF","OC")
        .put("WS","OC")
        .put("YE","AS")
        .put("YT","AF")
        .put("ZA","AF")
        .put("ZM","AF")
        .put("ZW","AF")
        .build();

}