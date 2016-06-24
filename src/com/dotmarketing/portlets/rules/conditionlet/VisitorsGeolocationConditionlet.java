package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * Conditionlet expects all input to be in Meters. The unit type exists to inform the UI what should be displayed to the user
 */
public class VisitorsGeolocationConditionlet extends Conditionlet<VisitorsGeolocationConditionlet.Instance>{

    public enum UnitOfDistance {
        MILES("mi"),
        METERS("m"),
        KILOMETERS("km");

        public final String abbreviation;

        UnitOfDistance(String abbreviation) {
            this.abbreviation = abbreviation;
        }

        static UnitOfDistance fromAbbreviation(String abbreviation){
            UnitOfDistance result = null;
            for (UnitOfDistance unit : UnitOfDistance.values()) {
                if(unit.abbreviation.equals(abbreviation)){
                    result = unit;
                    break;
                }
            }
            if(result == null){
                throw new RuleEngineException("No UnitOfDistance with abbreviation '%s'", abbreviation);
            }
            return result;
        }

    }
	private static final long serialVersionUID = 1L;

    public static final String RADIUS_KEY = "radius";
    public static final String RADIUS_UNIT_KEY = "preferredDisplayUnits";
    public static final String LATITUDE_KEY = "latitude";
    public static final String LONGITUDE_KEY = "longitude";

    private final GeoIp2CityDbUtil geoIp2Util;

    private static final ParameterDefinition<NumericType> distance = new ParameterDefinition<>(
        3, RADIUS_KEY, new NumericInput<>(new NumericType().required().minValue(10).maxValue(40000*1000).defaultValue(100*1000)));

    private static final ParameterDefinition<TextType> unitOfDistance = new ParameterDefinition<>(
        4, RADIUS_UNIT_KEY, new DropdownInput(new TextType().required().maxLength(10))
            .minSelections(1)
            .maxSelections(1)
            .option(UnitOfDistance.MILES.abbreviation)
            .option(UnitOfDistance.METERS.abbreviation)
            .option(UnitOfDistance.KILOMETERS.abbreviation),
        UnitOfDistance.MILES.abbreviation
    );

    private static final ParameterDefinition<TextType> latitude = new ParameterDefinition<>(
            5, LATITUDE_KEY, new TextInput<>(new TextType().required().defaultValue("38.8977")));

    private static final ParameterDefinition<TextType> longitude = new ParameterDefinition<>(
            6, LONGITUDE_KEY, new TextInput<>(new TextType().required().defaultValue("-77.0365")));

    public VisitorsGeolocationConditionlet() {
        this(GeoIp2CityDbUtil.getInstance());
    }

    @VisibleForTesting
    VisitorsGeolocationConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super("api.ruleengine.system.conditionlet.VisitorsLocation",
              new ComparisonParameterDefinition(2, WITHIN_DISTANCE, NOT_WITHIN_DISTANCE),
                distance, unitOfDistance, latitude, longitude);
        this.geoIp2Util = geoIp2Util;
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        Location visitorsLocation = lookupLocation(request);
        Location inputLocation = new Location(instance.latitude, instance.longitude);
        //noinspection unchecked
        return instance.comparison.perform(visitorsLocation, inputLocation, instance.distance);
    }

    private Location lookupLocation(HttpServletRequest request) {
        try {
            InetAddress address = HttpRequestDataUtil.getIpAddress(request);
            String ipAddress = address.getHostAddress();
            return geoIp2Util.getLocationByIp(ipAddress);
        } catch (UnknownHostException e) {
            throw new RuleEvaluationFailedException(e, "Unknown host.");
        } catch (IOException | GeoIp2Exception e) {
            throw new RuleEvaluationFailedException(e, "Unable to get Location from IP. ");
        }
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final double distance;
        public final UnitOfDistance unitOfDistance;
        public final double latitude;
        public final double longitude;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 5,
                       "Visitors Location Condition requires parameters %s, %s, %s, %s and %s.", COMPARISON_KEY,
                       RADIUS_KEY, RADIUS_UNIT_KEY, LATITUDE_KEY, LONGITUDE_KEY );
            assert parameters != null;
            this.distance = Double.parseDouble(parameters.get(RADIUS_KEY).getValue());
            this.unitOfDistance = UnitOfDistance.fromAbbreviation(parameters.get(RADIUS_UNIT_KEY).getValue());
            this.latitude = Double.parseDouble(parameters.get(LATITUDE_KEY).getValue());
            this.longitude = Double.parseDouble(parameters.get(LONGITUDE_KEY).getValue());
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                this.comparison = ((ComparisonParameterDefinition)definition
                    .getParameterDefinitions()
                    .get(COMPARISON_KEY))
                    .comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }


}
