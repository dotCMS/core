package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
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

public class VisitorsGeolocationConditionlet extends Conditionlet<VisitorsGeolocationConditionlet.Instance>{

	private static final long serialVersionUID = 1L;

    public static final String DISTANCE_KEY = "distance";
    public static final String UNIT_OF_DISTANCE_KEY = "unit-of-distance";
    public static final String LATITUDE_KEY = "latitude";
    public static final String LONGITUDE_KEY = "longitude";

    private final GeoIp2CityDbUtil geoIp2Util;

    private static final ParameterDefinition<NumericType> distance = new ParameterDefinition<>(
        3, DISTANCE_KEY, new NumericInput<>(new NumericType()));

    private static final ParameterDefinition<TextType> unitOfDistance = new ParameterDefinition<>(
        4, UNIT_OF_DISTANCE_KEY, new DropdownInput(new TextType().maxLength(10))
            .minSelections(1)
            .maxSelections(1)
            .option(Location.UnitOfDistance.MILES.name())
            .option(Location.UnitOfDistance.KILOMETERS.name()),
            Location.UnitOfDistance.MILES.name()
    );

    private static final ParameterDefinition<TextType> latitude = new ParameterDefinition<>(
            5, LATITUDE_KEY, new TextInput<>(new TextType()));

    private static final ParameterDefinition<TextType> longitude = new ParameterDefinition<>(
            6, LONGITUDE_KEY, new TextInput<>(new TextType()));

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
        return instance.comparison.perform(visitorsLocation, inputLocation, instance.distance, instance.unitOfDistance);
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
        public final Location.UnitOfDistance unitOfDistance;
        public final double latitude;
        public final double longitude;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 5,
                       "Visitors Location Condition requires parameters %s, %s, %s, %s and %s.", COMPARISON_KEY, DISTANCE_KEY, UNIT_OF_DISTANCE_KEY, LATITUDE_KEY, LONGITUDE_KEY );
            assert parameters != null;
            this.distance = Double.parseDouble(parameters.get(DISTANCE_KEY).getValue());
            this.unitOfDistance = Location.UnitOfDistance.valueOf(parameters.get(UNIT_OF_DISTANCE_KEY).getValue());
            this.latitude = Double.parseDouble(parameters.get(LATITUDE_KEY).getValue());
            this.longitude = Double.parseDouble(parameters.get(LONGITUDE_KEY).getValue());
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


}
