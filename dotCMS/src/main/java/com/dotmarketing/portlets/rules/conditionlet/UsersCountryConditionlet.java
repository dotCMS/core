package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

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
 */
public class UsersCountryConditionlet extends Conditionlet<UsersCountryConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String COUNTRY_KEY = "country";

    private final GeoIp2CityDbUtil geoIp2Util;

    private static final DropdownInput countries = new DropdownInput()
        .minSelections(1)
        .option("AF")
        .option("AL")
        .option("DZ")
        .option("AS")
        .option("AD")
        .option("AO")
        .option("AI")
        .option("AQ")
        .option("AR")
        .option("AM")
        .option("AW")
        .option("AU")
        .option("AT")
        .option("AZ")
        .option("BS")
        .option("BH")
        .option("BD")
        .option("BB")
        .option("BY")
        .option("BE")
        .option("BZ")
        .option("BJ")
        .option("BM")
        .option("BT")
        .option("BO")
        .option("BA")
        .option("BW")
        .option("BR")
        .option("VG")
        .option("BN")
        .option("BG")
        .option("BF")
        .option("BI")
        .option("KH")
        .option("CM")
        .option("CA")
        .option("CV")
        .option("KY")
        .option("CF")
        .option("CL")
        .option("CN")
        .option("CO")
        .option("KM")
        .option("CK")
        .option("CR")
        .option("HR")
        .option("CU")
        .option("CW")
        .option("CY")
        .option("CZ")
        .option("CD")
        .option("DK")
        .option("DJ")
        .option("DM")
        .option("DO")
        .option("TL")
        .option("EC")
        .option("EG")
        .option("SV")
        .option("GQ")
        .option("ER")
        .option("EE")
        .option("ET")
        .option("FK")
        .option("FO")
        .option("FJ")
        .option("FI")
        .option("FR")
        .option("PF")
        .option("GA")
        .option("GM")
        .option("GE")
        .option("DE")
        .option("GH")
        .option("GI")
        .option("GR")
        .option("GL")
        .option("GP")
        .option("GU")
        .option("GT")
        .option("GN")
        .option("GW")
        .option("GY")
        .option("HT")
        .option("HN")
        .option("HK")
        .option("HU")
        .option("IS")
        .option("IN")
        .option("ID")
        .option("IR")
        .option("IQ")
        .option("IE")
        .option("IM")
        .option("IL")
        .option("IT")
        .option("CI")
        .option("JM")
        .option("JP")
        .option("JO")
        .option("KZ")
        .option("KE")
        .option("KI")
        .option("XK")
        .option("KW")
        .option("KG")
        .option("LA")
        .option("LV")
        .option("LB")
        .option("LS")
        .option("LR")
        .option("LY")
        .option("LI")
        .option("LT")
        .option("LU")
        .option("MO")
        .option("MK")
        .option("MG")
        .option("MW")
        .option("MY")
        .option("MV")
        .option("ML")
        .option("MT")
        .option("MH")
        .option("MR")
        .option("MU")
        .option("MX")
        .option("FM")
        .option("MD")
        .option("MC")
        .option("MN")
        .option("ME")
        .option("MS")
        .option("MA")
        .option("MZ")
        .option("MM")
        .option("NA")
        .option("NR")
        .option("NP")
        .option("NL")
        .option("NC")
        .option("NZ")
        .option("NI")
        .option("NE")
        .option("NG")
        .option("NU")
        .option("NF")
        .option("KP")
        .option("MP")
        .option("NO")
        .option("OM")
        .option("PK")
        .option("PW")
        .option("PA")
        .option("PG")
        .option("PY")
        .option("PE")
        .option("PH")
        .option("PN")
        .option("PL")
        .option("PT")
        .option("PR")
        .option("QA")
        .option("CG")
        .option("RE")
        .option("RO")
        .option("RU")
        .option("RW")
        .option("BL")
        .option("SH")
        .option("KN")
        .option("LC")
        .option("MF")
        .option("PM")
        .option("VC")
        .option("WS")
        .option("SM")
        .option("ST")
        .option("SA")
        .option("SN")
        .option("RS")
        .option("SC")
        .option("SL")
        .option("SG")
        .option("SK")
        .option("SI")
        .option("SB")
        .option("SO")
        .option("ZA")
        .option("KR")
        .option("SS")
        .option("ES")
        .option("LK")
        .option("SD")
        .option("SR")
        .option("SZ")
        .option("SE")
        .option("CH")
        .option("SY")
        .option("TW")
        .option("TJ")
        .option("TZ")
        .option("TH")
        .option("TG")
        .option("TK")
        .option("TT")
        .option("TN")
        .option("TR")
        .option("TM")
        .option("TV")
        .option("UG")
        .option("UA")
        .option("AE")
        .option("GB")
        .option("US")
        .option("UY")
        .option("UZ")
        .option("VU")
        .option("VA")
        .option("VE")
        .option("VN")
        .option("EH")
        .option("YE")
        .option("ZM")
        .option("ZW");

    static {
        for (DropdownInput.Option option : countries.getOptions().values()) {
            option.icon("flag " + option.value.toLowerCase());
        }
    }

    private static final ParameterDefinition<TextType> country = new ParameterDefinition<>(
        3, COUNTRY_KEY, "system.locale.country",
        countries,
        ""
    );


    public UsersCountryConditionlet() {
        this(GeoIp2CityDbUtil.getInstance());
    }

    @VisibleForTesting
    UsersCountryConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super("api.system.ruleengine.conditionlet.VisitorCountry",
              new ComparisonParameterDefinition(2, IS, IS_NOT),
              country);
        this.geoIp2Util = geoIp2Util;
    }

    /**
     * Instance is guaranteed to be valid.
     */
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String requestCountry = lookupCountry(request);
        return instance.comparison.perform(requestCountry, instance.countryCode);
    }

    private String lookupCountry(HttpServletRequest request) {
        String country = "unknown";
        InetAddress address;
        try {
            address = HttpRequestDataUtil.getIpAddress(request);
        } catch (UnknownHostException e) {
            throw new RuleEvaluationFailedException(e, "Unknown host.");
        }
        String ipAddress = address.getHostAddress();
        try {
            country = geoIp2Util.getCountryIsoCode(ipAddress);
        } catch (IOException | GeoIp2Exception e) {
            Logger.debug(this, "Could not look up country for request. Using 'unknown': " + request.getRequestURL());
        }
        return country;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        private final String countryCode;
        private final Comparison<String> comparison;

        private Instance(UsersCountryConditionlet definition, Map<String, ParameterModel> parameters) {
            this.countryCode = parameters.get(COUNTRY_KEY).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                //noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }
}
