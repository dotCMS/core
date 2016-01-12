package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.unittest.TestUtil;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.UsersCityConditionlet.CITY_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EXISTS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsersCityConditionletTest {

    private static final String MOCK_IP_ADDRESS = "170.123.234.133"; //Albany

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @DataProvider(name = "cases")
    public Object[][] compareCases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


        /* Is */
            data.add(new TestCase("If Ip resolves to Albany and Albany is in values, evaluate to true.")
                         .withComparison(IS)
                         .withCityName("Albany")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToCityName(MOCK_IP_ADDRESS, "Albany")
                         .shouldBeTrue()
            );

            data.add(new TestCase("If Ip resolves to Albany and Boston is specified, evaluate to false.")
                         .withComparison(IS)
                         .withCityName("Boston")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToCityName(MOCK_IP_ADDRESS, "Albany")
                         .shouldBeFalse()
            );

            data.add(new TestCase("If Ip resolves to 'unknown' and Boston is specified, evaluate to false.")
                         .withComparison(IS)
                         .withCityName("Boston")
                         .withRequestIpAddress("localhost")
                         .withMockIpToCityName("localhost", "unknown")
                         .shouldBeFalse()
            );

            /* Is Not*/
            data.add(new TestCase("Is Not: If Ip resolves to Albany and Albany is specified, evaluate to false.")
                         .withComparison(IS_NOT)
                         .withCityName("Albany")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToCityName(MOCK_IP_ADDRESS, "Albany")
                         .shouldBeFalse()
            );

            data.add(new TestCase("Is Not: If Ip resolves to Albany and Boston is specified, evaluate to true.")
                         .withComparison(IS_NOT)
                         .withCityName("Boston")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToCityName(MOCK_IP_ADDRESS, "Albany")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Is Not: If Ip resolves to 'unknown' and Boston is specified, evaluate to true.")
                         .withComparison(IS_NOT)
                         .withCityName("Boston")
                         .withRequestIpAddress("localhost")
                         .withMockIpToCityName("localhost", "unknown")
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

    @Test(expectedExceptions = NullPointerException.class)
    public void testEvaluatesToFalseWhenArgumentsAreEmptyOrMissing() throws Exception {
        new TestCase("").conditionlet.instanceFrom(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testCannotValidateWhenComparisonIsNull() throws Exception {
        TestCase aCase = new TestCase("Empty parameter list should throw NPE.").withComparison(null);
        new TestCase("").conditionlet.instanceFrom(aCase.params);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testCannotValidateWhenComparisonNotSet() throws Exception {
        new TestCase("").conditionlet.instanceFrom(Maps.newHashMap());
    }

    @Test
    public void testSingleCountryEvaluatesFalseIfIpCannotBeResolved() throws Exception {
        TestCase aCase = new TestCase("If Ip does not resolve, evaluate to false.")
            .withComparison(IS)
            .withCityName("Boston")
            .withRequestIpAddress(MOCK_IP_ADDRESS)
            .shouldBeFalse();
        when(aCase.geoIp2Util.getCountryIsoCode(MOCK_IP_ADDRESS)).thenThrow(new GeoIp2Exception("Boom."));
        assertThat(aCase.testDescription, runCase(aCase), is(false));
    }

    @Test(expectedExceptions = ComparisonNotSupportedException.class)
    public void testUnsupportedComparisonThrowsException() throws Exception {
        TestCase aCase = new TestCase("Exists: Unsupported comparison should throw.")
            .withComparison(EXISTS)
            .withCityName("Albany")
            .withRequestIpAddress(MOCK_IP_ADDRESS)
            .withMockIpToCityName(MOCK_IP_ADDRESS, "Albany")
            .shouldBeFalse();
        runCase(aCase);
    }

    private class TestCase {

        public final UsersCityConditionlet conditionlet;
        public final GeoIp2CityDbUtil geoIp2Util = mock(GeoIp2CityDbUtil.class);
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;

        private boolean expect;

        public TestCase(String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
            conditionlet = new UsersCityConditionlet(geoIp2Util);
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

        TestCase withMockIpToCityName(String mockIpAddress, String mockCityName) throws IOException, GeoIp2Exception {
            when(geoIp2Util.getCityName(mockIpAddress)).thenReturn(mockCityName);
            return this;
        }

        TestCase withCityName(String isoCode) {
            params.put(CITY_KEY, new ParameterModel(CITY_KEY, isoCode));
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
