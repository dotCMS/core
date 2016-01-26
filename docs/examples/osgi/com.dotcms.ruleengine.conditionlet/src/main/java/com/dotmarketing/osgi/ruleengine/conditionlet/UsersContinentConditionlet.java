package com.dotmarketing.osgi.ruleengine.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.conditionlet.ComparisonParameterDefinition;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

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
 */
public class UsersContinentConditionlet extends Conditionlet<UsersContinentConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    private static final String CONTINENT_CODE_KEY = "continent";

    private static final String I18N_BASE = "com.dotmarketing.osgi.ruleengine.conditionlet";
    private static final String SYSTEM_LOCATE_CONTINENT_KEY = "com.dotmarketing.osgi.ruleengine.conditionlet.continent";
    
    private final GeoIp2CityDbUtil geoIp2Util;

    private static final ParameterDefinition<TextType> continentKey = new ParameterDefinition<>(
        1, CONTINENT_CODE_KEY,SYSTEM_LOCATE_CONTINENT_KEY,
        new DropdownInput()
            .minSelections(1)
            .option("af")
            .option("an")
            .option("as")
            .option("eu")
            .option("na")
            .option("oc")
            .option("sa"),
        "na"
    );



    public UsersContinentConditionlet() {
        this(GeoIp2CityDbUtil.getInstance());
    }

    @VisibleForTesting
    protected UsersContinentConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super(I18N_BASE,
              new ComparisonParameterDefinition(2, IS, IS_NOT),
              continentKey);
        this.geoIp2Util = geoIp2Util;
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {

        String visitorsCountryCode = getVisitorsCountryCode(request);
        String visitorContinent = continentCountryMap.get(visitorsCountryCode);
        if(visitorContinent == null) {
            throw new ContinentCouldNotBeDeterminedException(visitorsCountryCode);
        }
        //noinspection unchecked
        return instance.comparison.perform(visitorContinent, instance.continentCode);
    }

    private String getVisitorsCountryCode(HttpServletRequest request) {
        String country = null;
        try {
            InetAddress address = HttpRequestDataUtil.getIpAddress(request);
            String ipAddress = address.getHostAddress();
            if(ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost")){
				country = request.getLocale().getCountry();
			}else{
				country = geoIp2Util.getCountryIsoCode(ipAddress);
			}
        } catch (IOException | GeoIp2Exception e) {
            Logger.error(this,
                         "An error occurred when retrieving the IP address from request: "
                         + request.getRequestURL());
        }
        return country;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String continentCode;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 2,
                       "Visitor's Continent Condition requires parameters %s and %s.", COMPARISON_KEY, CONTINENT_CODE_KEY);
            assert parameters != null;
            this.continentCode = parameters.get(CONTINENT_CODE_KEY).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();

            try {
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }

    static final Map<String, String> continentCountryMap = ImmutableMap.<String, String>builder()
        .put("AD", "eu")
        .put("AE", "as")
        .put("af", "as")
        .put("AG", "na")
        .put("AI", "na")
        .put("AL", "eu")
        .put("AM", "as")
        .put("an", "na")
        .put("AO", "af")
        .put("AP", "as")
        .put("AQ", "an")
        .put("AR", "sa")
        .put("as", "oc")
        .put("AT", "eu")
        .put("AU", "oc")
        .put("AW", "na")
        .put("AX", "eu")
        .put("AZ", "as")
        .put("BA", "eu")
        .put("BB", "na")
        .put("BD", "as")
        .put("BE", "eu")
        .put("BF", "af")
        .put("BG", "eu")
        .put("BH", "as")
        .put("BI", "af")
        .put("BJ", "af")
        .put("BL", "na")
        .put("BM", "na")
        .put("BN", "as")
        .put("BO", "sa")
        .put("BR", "sa")
        .put("BS", "na")
        .put("BT", "as")
        .put("BV", "an")
        .put("BW", "af")
        .put("BY", "eu")
        .put("BZ", "na")
        .put("CA", "na")
        .put("CC", "as")
        .put("CD", "af")
        .put("CF", "af")
        .put("CG", "af")
        .put("CH", "eu")
        .put("CI", "af")
        .put("CK", "oc")
        .put("CL", "sa")
        .put("CM", "af")
        .put("CN", "as")
        .put("CO", "sa")
        .put("CR", "na")
        .put("CU", "na")
        .put("CV", "af")
        .put("CX", "as")
        .put("CY", "as")
        .put("CZ", "eu")
        .put("DE", "eu")
        .put("DJ", "af")
        .put("DK", "eu")
        .put("DM", "na")
        .put("DO", "na")
        .put("DZ", "af")
        .put("EC", "sa")
        .put("EE", "eu")
        .put("EG", "af")
        .put("EH", "af")
        .put("ER", "af")
        .put("ES", "eu")
        .put("ET", "af")
        .put("eu", "eu")
        .put("FI", "eu")
        .put("FJ", "oc")
        .put("FK", "sa")
        .put("FM", "oc")
        .put("FO", "eu")
        .put("FR", "eu")
        .put("FX", "eu")
        .put("GA", "af")
        .put("GB", "eu")
        .put("GD", "na")
        .put("GE", "as")
        .put("GF", "sa")
        .put("GG", "eu")
        .put("GH", "af")
        .put("GI", "eu")
        .put("GL", "na")
        .put("GM", "af")
        .put("GN", "af")
        .put("GP", "na")
        .put("GQ", "af")
        .put("GR", "eu")
        .put("GS", "an")
        .put("GT", "na")
        .put("GU", "oc")
        .put("GW", "af")
        .put("GY", "sa")
        .put("HK", "as")
        .put("HM", "an")
        .put("HN", "na")
        .put("HR", "eu")
        .put("HT", "na")
        .put("HU", "eu")
        .put("ID", "as")
        .put("IE", "eu")
        .put("IL", "as")
        .put("IM", "eu")
        .put("IN", "as")
        .put("IO", "as")
        .put("IQ", "as")
        .put("IR", "as")
        .put("IS", "eu")
        .put("IT", "eu")
        .put("JE", "eu")
        .put("JM", "na")
        .put("JO", "as")
        .put("JP", "as")
        .put("KE", "af")
        .put("KG", "as")
        .put("KH", "as")
        .put("KI", "oc")
        .put("KM", "af")
        .put("KN", "na")
        .put("KP", "as")
        .put("KR", "as")
        .put("KW", "as")
        .put("KY", "na")
        .put("KZ", "as")
        .put("LA", "as")
        .put("LB", "as")
        .put("LC", "na")
        .put("LI", "eu")
        .put("LK", "as")
        .put("LR", "af")
        .put("LS", "af")
        .put("LT", "eu")
        .put("LU", "eu")
        .put("LV", "eu")
        .put("LY", "af")
        .put("MA", "af")
        .put("MC", "eu")
        .put("MD", "eu")
        .put("ME", "eu")
        .put("MF", "na")
        .put("MG", "af")
        .put("MH", "oc")
        .put("MK", "eu")
        .put("ML", "af")
        .put("MM", "as")
        .put("MN", "as")
        .put("MO", "as")
        .put("MP", "oc")
        .put("MQ", "na")
        .put("MR", "af")
        .put("MS", "na")
        .put("MT", "eu")
        .put("MU", "af")
        .put("MV", "as")
        .put("MW", "af")
        .put("MX", "na")
        .put("MY", "as")
        .put("MZ", "af")
        .put("na", "af")
        .put("NC", "oc")
        .put("NE", "af")
        .put("NF", "oc")
        .put("NG", "af")
        .put("NI", "na")
        .put("NL", "eu")
        .put("NO", "eu")
        .put("NP", "as")
        .put("NR", "oc")
        .put("NU", "oc")
        .put("NZ", "oc")
        .put("OM", "as")
        .put("PA", "na")
        .put("PE", "sa")
        .put("PF", "oc")
        .put("PG", "oc")
        .put("PH", "as")
        .put("PK", "as")
        .put("PL", "eu")
        .put("PM", "na")
        .put("PN", "oc")
        .put("PR", "na")
        .put("PS", "as")
        .put("PT", "eu")
        .put("PW", "oc")
        .put("PY", "sa")
        .put("QA", "as")
        .put("RE", "af")
        .put("RO", "eu")
        .put("RS", "eu")
        .put("RU", "eu")
        .put("RW", "af")
        .put("sa", "as")
        .put("SB", "oc")
        .put("SC", "af")
        .put("SD", "af")
        .put("SE", "eu")
        .put("SG", "as")
        .put("SH", "af")
        .put("SI", "eu")
        .put("SJ", "eu")
        .put("SK", "eu")
        .put("SL", "af")
        .put("SM", "eu")
        .put("SN", "af")
        .put("SO", "af")
        .put("SR", "sa")
        .put("ST", "af")
        .put("SV", "na")
        .put("SY", "as")
        .put("SZ", "af")
        .put("TC", "na")
        .put("TD", "af")
        .put("TF", "an")
        .put("TG", "af")
        .put("TH", "as")
        .put("TJ", "as")
        .put("TK", "oc")
        .put("TL", "as")
        .put("TM", "as")
        .put("TN", "af")
        .put("TO", "oc")
        .put("TR", "eu")
        .put("TT", "na")
        .put("TV", "oc")
        .put("TW", "as")
        .put("TZ", "af")
        .put("UA", "eu")
        .put("UG", "af")
        .put("UM", "oc")
        .put("US", "na")
        .put("UY", "sa")
        .put("UZ", "as")
        .put("VA", "eu")
        .put("VC", "na")
        .put("VE", "sa")
        .put("VG", "na")
        .put("VI", "na")
        .put("VN", "as")
        .put("VU", "oc")
        .put("WF", "oc")
        .put("WS", "oc")
        .put("YE", "as")
        .put("YT", "af")
        .put("ZA", "af")
        .put("ZM", "af")
        .put("ZW", "af")
        .build();
}