package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.unittest.TestUtil;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.VisitorsGeolocationConditionlet.*;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisitorsGeolocationConditionletTest {

    private static final String MOCK_IP_ADDRESS = "190.74.5.100";
    private static final double LATITUDE = 10.4883717;
    private static final double LONGITUDE = -66.8799873;
    private static final double VISITORS_LATITUDE = 8.0;
    private static final double VISITORS_LONGITUDE = -66.0;
    private static final double DISTANCE = 30;

    @DataProvider(name = "cases")
    public Object[][] compareCases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();

            /* WITHIN DISTANCE */
            data.add(new TestCase("When Visitors location is ("+VISITORS_LATITUDE+", "+LONGITUDE+") and comparison is WITHIN and distance is 30 KM of location ("+LATITUDE+", "+LONGITUDE+"), should evaluate to false")
                    .withComparison(WITHIN_DISTANCE)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withMockVisitorsLocation(VISITORS_LATITUDE, VISITORS_LONGITUDE)
                    .withDistance(DISTANCE)
                    .withUnitOfDistance(Location.UnitOfDistance.KILOMETERS)
                    .withLatitude(LATITUDE)
                    .withLongitude(LONGITUDE)
                    .shouldBeFalse()
            );

            /* NOT WITHIN DISTANCE */
            data.add(new TestCase("When Visitors location is ("+VISITORS_LATITUDE+", "+LONGITUDE+") and comparison is NOT WITHIN and distance is 30 KM of location ("+LATITUDE+", "+LONGITUDE+"), should evaluate to true")
                    .withComparison(NOT_WITHIN_DISTANCE)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withMockVisitorsLocation(VISITORS_LATITUDE, VISITORS_LONGITUDE)
                    .withDistance(DISTANCE)
                    .withUnitOfDistance(Location.UnitOfDistance.KILOMETERS)
                    .withLatitude(LATITUDE)
                    .withLongitude(LONGITUDE)
                    .shouldBeTrue()
            );

            return TestUtil.toCaseArray(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test(dataProvider = "cases")
    public void testComparisons(TestCase aCase) throws Exception {
        assertThat(aCase.testDescription, runCase(aCase), is(aCase.expect));
    }

    private boolean runCase(TestCase aCase) {
        return aCase.conditionlet.evaluate(aCase.request, aCase.response, aCase.conditionlet.instanceFrom(aCase.params));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testEvaluatesToFalseWhenArgumentsAreEmptyOrMissing() throws Exception {
        new TestCase("").conditionlet.instanceFrom(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testCannotValidateWhenComparisonIsNull() throws Exception {
        TestCase aCase = new TestCase("Empty parameter list should throw IAE.").withComparison(null);
        new TestCase("").conditionlet.instanceFrom(aCase.params);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testCannotValidateWhenComparisonNotSet() throws Exception {
        new TestCase("").conditionlet.instanceFrom(Maps.newHashMap());
    }

    @Test(expectedExceptions = ComparisonNotSupportedException.class)
    public void testUnsupportedComparisonThrowsException() throws Exception {
        TestCase aCase = new TestCase("Exists: Unsupported comparison should throw.")
                .withComparison(EXISTS)
                .withDistance(DISTANCE)
                .withUnitOfDistance(Location.UnitOfDistance.KILOMETERS)
                .withLatitude(LATITUDE)
                .withLongitude(LONGITUDE)
                .shouldBeFalse();
        runCase(aCase);
    }

    private class TestCase {

        public final VisitorsGeolocationConditionlet conditionlet;
        public final GeoIp2CityDbUtil geoIp2Util = mock(GeoIp2CityDbUtil.class);

        private final HttpServletRequest request ;
        private final HttpServletResponse response;

        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;

        private boolean expect;

        public TestCase(String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
            conditionlet = new VisitorsGeolocationConditionlet(geoIp2Util);
        }

        TestCase shouldBeTrue() {
            this.expect = true;
            return this;
        }

        TestCase shouldBeFalse() {
            this.expect = false;
            return this;
        }

        TestCase withComparison(Comparison c) {
            params.put(COMPARISON_KEY, new ParameterModel(COMPARISON_KEY, c != null ? c.getId() : null));
            return this;
        }

        TestCase withRequestIpAddress(String mockIpAddress) throws IOException, GeoIp2Exception {
            when(request.getHeader("X-Forwarded-For")).thenReturn(mockIpAddress);
            return this;
        }

        TestCase withMockVisitorsLocation(double latitude, double longitude) throws IOException, GeoIp2Exception {
            Location location = new Location(latitude, longitude);
            when(geoIp2Util.getLocationByIp(MOCK_IP_ADDRESS)).thenReturn(location);
            return this;
        }

        TestCase withDistance(double distance) {
            params.put(DISTANCE_KEY, new ParameterModel(DISTANCE_KEY, Double.toString(distance)));
            return this;
        }

        TestCase withUnitOfDistance(Location.UnitOfDistance unitOfDistance) {
            params.put(UNIT_OF_DISTANCE_KEY, new ParameterModel(UNIT_OF_DISTANCE_KEY, unitOfDistance.name()));
            return this;
        }

        TestCase withLatitude(double latitude) {
            params.put(LATITUDE_KEY, new ParameterModel(LATITUDE_KEY, Double.toString(latitude)));
            return this;
        }

        TestCase withLongitude(double longitude) {
            params.put(LONGITUDE_KEY, new ParameterModel(LONGITUDE_KEY, Double.toString(longitude)));
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
