package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
//import com.dotmarketing.portlets.rules.ValidationResult;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
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
 * @author Jose Castro, Mauricio Rizo
 * @version 1.0
 * @since 04-16-2015
 *
 */
public class UsersCityConditionlet extends Conditionlet<UsersCityConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	public static final String CITY_KEY = "city";

	private final GeoIp2CityDbUtil geoIp2Util;

	// List of possible cities (USA)
	private static final DropdownInput cities = new DropdownInput()
			.allowAdditions()
			.option("Albany")
			.option("Annapolis")
			.option("Atlanta")
			.option("Augusta")
			.option("Austin")
			.option("Baton Rouge")
			.option("Bismark")
			.option("Boise")
			.option("Boston")
			.option("Carson City")
			.option("Charleston")
			.option("Cheyenne")
			.option("Columbia")
			.option("Columbus")
			.option("Concord")
			.option("Denver")
			.option("Des Moines")
			.option("Dover")
			.option("Frankfort")
			.option("Harrisburg")
			.option("Hartford")
			.option("Helena")
			.option("Honolulu")
			.option("Indianapolis")
			.option("Jackson")
			.option("Jefferson City")
			.option("Juneau")
			.option("Lansing")
			.option("Lincoln")
			.option("Little Rock")
			.option("Madison")
			.option("Montgomery")
			.option("Montpelier")
			.option("Nashville")
			.option("Oklahoma City")
			.option("Olympia")
			.option("Phoenix")
			.option("Pierre")
			.option("Providence")
			.option("Raleigh")
			.option("Richmond")
			.option("Sacramento")
			.option("Saint Paul")
			.option("Salem")
			.option("Salt Lake City")
			.option("Santa Fe")
			.option("Springfield")
			.option("Tallahassee")
			.option("Topeka")
			.option("Trenton");

	// Parameter definition, "city" is the text displayed on the dropdown as placeholder
	// and the list of cities, "" (empty) is the default value
	private static final ParameterDefinition<TextType> city = new ParameterDefinition<>(
	        3, CITY_KEY, "",
	        cities,
	        ""
	    );

	public UsersCityConditionlet() {
        this(GeoIp2CityDbUtil.getInstance());
    }

	// User city visitor, with 2 comparison parameters and a list of cities
	public UsersCityConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super("api.system.ruleengine.conditionlet.VisitorCity",
                new ComparisonParameterDefinition(2, IS, IS_NOT),
                city);
        this.geoIp2Util = geoIp2Util;
    }

	@Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {

		String city = null;
		try {
			InetAddress address = HttpRequestDataUtil.getIpAddress(request);
			String ipAddress = address.getHostAddress();
			if(ipAddress != null)
				city = geoIp2Util.getCityName(ipAddress);
		} catch (IOException | GeoIp2Exception e) {
			Logger.error(this,
					"An error occurred when retrieving the IP address from request: "
							+ request.getRequestURL());
		}
		// if city is null due to a failed attempt to get the city name from the IP
		if (!UtilMethods.isSet(city)) {
			city = "unknown";
		}

        return instance.comparison.perform(city, instance.cityName);
	}

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    // Instance definition
    public static class Instance implements RuleComponentInstance {

    	private final String cityName;
    	private final Comparison<String> comparison;

        private Instance(UsersCityConditionlet definition, Map<String, ParameterModel> parameters) {
        	this.cityName = parameters.get(CITY_KEY).getValue();
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
