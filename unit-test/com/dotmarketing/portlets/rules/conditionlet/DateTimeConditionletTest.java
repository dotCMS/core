package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import org.elasticsearch.common.joda.time.LocalDateTime;
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
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.DateTimeConditionlet.*;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateTimeConditionletTest {

    private static final String MOCK_IP_ADDRESS = "10.0.0.1";

    @DataProvider(name = "cases")
    public Object[][] compareCases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


            /* EQUAL */
            data.add(new TestCase("If 2016-01-01T00:00 set and visitor's datetime is 2016-01-01T00:00 , evaluate to true.")
                    .withComparison(EQUAL)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T00:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T00:00")
                    .shouldBeTrue()
            );

            /* EQUAL */
            data.add(new TestCase("If 2016-01-01T00:00 set and visitor's datetime is 2016-01-01T00:01 , evaluate to false.")
                    .withComparison(EQUAL)
                    .withRequestIpAddress(MOCK_IP_ADDRESS)
                    .withDateTime1("2016-01-01T00:00")
                    .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T00:01")
                    .shouldBeFalse()
            );


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
                .withDateTime1("2016-01-01T00:00")
                .withMockVisitorsDateTime(MOCK_IP_ADDRESS, "2016-01-01T00:00")
                .shouldBeFalse();
        runCase(aCase);
    }

    private class TestCase {

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
            when(request.getHeader("X-Forwarded-For")).thenReturn(mockIpAddress);
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
