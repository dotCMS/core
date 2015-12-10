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
import static com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet.COUNTRY_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EXISTS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsersCountryConditionletTest {

    private static final String MOCK_IP_ADDRESS = "10.0.0.1";

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @DataProvider(name = "cases")
    public Object[][] compareCases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


        /* Is */
            data.add(new TestCase("If Ip resolves to GB and GB is in values, evaluate to true.")
                         .withComparison(IS)
                         .withIsoCode("GB")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB")
                         .shouldBeTrue()
            );

            data.add(new TestCase("If Ip resolves to GB and US is specified, evaluate to false.")
                         .withComparison(IS)
                         .withIsoCode("US")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB")
                         .shouldBeFalse()
            );

            data.add(new TestCase("If Ip resolves to 'unknown' and US is specified, evaluate to false.")
                         .withComparison(IS)
                         .withIsoCode("US")
                         .withRequestIpAddress("localhost")
                         .withMockIpToIsoCode("localhost", "unknown")
                         .shouldBeFalse()
            );

            /* Is Not*/
            data.add(new TestCase("Is Not: If Ip resolves to GB and GB is specified, evaluate to false.")
                         .withComparison(IS_NOT)
                         .withIsoCode("GB")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB")
                         .shouldBeFalse()
            );

            data.add(new TestCase("Is Not: If Ip resolves to GB and US is specified, evaluate to true.")
                         .withComparison(IS_NOT)
                         .withIsoCode("US")
                         .withRequestIpAddress(MOCK_IP_ADDRESS)
                         .withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Is Not: If Ip resolves to 'unknown' and US is specified, evaluate to true.")
                         .withComparison(IS_NOT)
                         .withIsoCode("US")
                         .withRequestIpAddress("localhost")
                         .withMockIpToIsoCode("localhost", "unknown")
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
            .withIsoCode("US")
            .withRequestIpAddress(MOCK_IP_ADDRESS)
            .shouldBeFalse();
        when(aCase.geoIp2Util.getCountryIsoCode(MOCK_IP_ADDRESS)).thenThrow(new GeoIp2Exception("Boom."));
        assertThat(aCase.testDescription, runCase(aCase), is(false));
    }

    @Test(expectedExceptions = ComparisonNotSupportedException.class)
    public void testUnsupportedComparisonThrowsException() throws Exception {
        TestCase aCase = new TestCase("Exists: Unsupported comparison should throw.")
            .withComparison(EXISTS)
            .withIsoCode("GB")
            .withRequestIpAddress(MOCK_IP_ADDRESS)
            .withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB")
            .shouldBeFalse();
        runCase(aCase);
    }

    private class TestCase {

        public final UsersCountryConditionlet conditionlet;
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
            conditionlet = new UsersCountryConditionlet(geoIp2Util);
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

        TestCase withMockIpToIsoCode(String mockIpAddress, String mockIsoCode) throws IOException, GeoIp2Exception {
            when(geoIp2Util.getCountryIsoCode(mockIpAddress)).thenReturn(mockIsoCode);
            return this;
        }

        TestCase withIsoCode(String isoCode) {
            params.put(COUNTRY_KEY, new ParameterModel(COUNTRY_KEY, isoCode));
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
