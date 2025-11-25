package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.unittest.TestUtil;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;


import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.DateTimeConditionlet.DATE_TIME_1_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.DateTimeConditionlet.DATE_TIME_2_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.BETWEEN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EXISTS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class DateTimeConditionletTest extends UnitTestBase {

    private static final String MOCK_IP_ADDRESS = "10.0.0.1";

    @DataProvider
    public static Object[][] cases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


            /* GREATER THAN */
            data.add(new TestCase("If 2016-01-01T00:00 set and visitor's datetime is 2016-01-01T01:00 , evaluate to true.")
                    .withComparison(GREATER_THAN)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T00:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T01:00")
                    .shouldBeTrue()
            );

            /* GREATER THAN */
            data.add(new TestCase("If 2016-01-01T01:00 set and visitor's datetime is 2016-01-01T00:00 , evaluate to false.")
                    .withComparison(GREATER_THAN)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T01:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T00:00")
                    .shouldBeFalse()
            );

            /* LESS THAN */
            data.add(new TestCase("If 2016-01-01T01:00 set and visitor's datetime is 2016-01-01T00:00 , evaluate to true.")
                    .withComparison(LESS_THAN)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T01:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T00:00")
                    .shouldBeTrue()
            );

            /* LESS THAN */
            data.add(new TestCase("If 2016-01-01T00:00 set and visitor's datetime is 2016-01-01T01:00 , evaluate to true.")
                    .withComparison(LESS_THAN)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T00:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T01:00")
                    .shouldBeFalse()
            );

            /* BETWEEN */
            data.add(new TestCase("If 2016-01-01T00:00, 2016-01-01T02:00 set and visitor's datetime is 2016-01-01T01:00 , evaluate to true.")
                    .withComparison(BETWEEN)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T00:00")
                    .withDateTime2("2016-01-01T02:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T01:00")
                    .shouldBeTrue()
            );


            return TestUtil.toCaseArray(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @UseDataProvider("cases")
    public void testComparisons(TestCase aCase) throws Exception {
        assertThat(aCase.testDescription, runCase(aCase), is(aCase.expect));
    }

    private boolean runCase(TestCase aCase) {
        return aCase.conditionlet.evaluate(aCase.request, aCase.response, aCase.conditionlet.instanceFrom(aCase.params));
    }

    @Test(expected = IllegalStateException.class)
    public void testEvaluatesToFalseWhenArgumentsAreEmptyOrMissing() throws Exception {
        new TestCase("").conditionlet.instanceFrom(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotValidateWhenComparisonIsNull() throws Exception {
        TestCase aCase = new TestCase("Empty parameter list should throw IAE.").withComparison(null);
        new TestCase("").conditionlet.instanceFrom(aCase.params);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotValidateWhenComparisonNotSet() throws Exception {
        new TestCase("").conditionlet.instanceFrom(Maps.newHashMap());
    }


    @Test(expected = ComparisonNotSupportedException.class)
    public void testUnsupportedComparisonThrowsException() throws Exception {
        TestCase aCase = new TestCase("Exists: Unsupported comparison should throw.")
                .withComparison(EXISTS)
                .withDateTime1("2016-01-01T00:00")
                .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T00:00")
                .shouldBeFalse();
        runCase(aCase);
    }

    private static class TestCase {

        public final DateTimeConditionlet conditionlet;
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
            conditionlet = new DateTimeConditionlet(geoIp2Util);
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
            when(request.getRemoteAddr()).thenReturn(mockIpAddress);
            return this;
        }

        TestCase withMockVisitorsDateTime(String mockIpAddress, String visitorsDateTime) throws IOException, GeoIp2Exception {
            LocalDateTime dateTime = LocalDateTime.parse(visitorsDateTime);
            Calendar mockDate = Calendar.getInstance();
            mockDate.setTime(dateTime.toDate());
            when(geoIp2Util.getDateTime(mockIpAddress)).thenReturn(mockDate);
            return this;
        }

        TestCase withDateTime1(String dateTime1) {
            params.put(DATE_TIME_1_KEY, new ParameterModel(DATE_TIME_1_KEY, dateTime1));
            return this;
        }

        TestCase withDateTime2(String dateTime2) {
            params.put(DATE_TIME_2_KEY, new ParameterModel(DATE_TIME_2_KEY, dateTime2));
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
