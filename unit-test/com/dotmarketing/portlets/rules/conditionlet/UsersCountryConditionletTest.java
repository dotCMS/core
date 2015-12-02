package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.unittest.TestUtil;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsersCountryConditionletTest {

    private static final String MOCK_IP_ADDRESS = "10.0.0.1";
    private UsersCountryConditionlet conditionlet;
    private GeoIp2CityDbUtil geoIp2Util = mock(GeoIp2CityDbUtil.class);

    @BeforeMethod
    public void setUp() throws Exception {
        Mockito.reset(geoIp2Util);
        conditionlet = new UsersCountryConditionlet(geoIp2Util);
    }

    @DataProvider(name = "noConditionCases")
    public Object[][] noConditionCases() {
        List<CountryConditionletCase> data = Lists.newArrayList();

        data.add(new CountryConditionletCase("Empty ConditionValue list should evaluate to false.", "is", Lists.<ConditionValue>newArrayList()));
        data.add(new CountryConditionletCase("Null ConditionValue list should evaluate to false.", "is", null));
        List<ConditionValue> list = Lists.newArrayList();
        list.add(new ConditionValue());
        data.add(new CountryConditionletCase("Empty comparison should trigger false evaluation.", "", list));
        data.add(new CountryConditionletCase("Null comparison should trigger false evaluation.", null, list));

        return TestUtil.toCaseArray(data);
    }

    @Test(groups = {"unit"}, dataProvider = "noConditionCases")
    public void testEvaluatesToFalseWhenArgumentsAreEmptyOrMissing(CountryConditionletCase aCase) throws Exception {
        assertThat(aCase.testDescription, runCase(aCase), is(false));
    }

    @Test
    public void testSingleCountryEvaluatesTrueIfIpResolvesToIsoCodeInValuesForIs() throws Exception {
        CountryConditionletCase aCase = new CountryConditionletCase("If Ip resolves to GB and GB is in values, evaluate to true.", "is");
        aCase.withIsoCode("GB").withRequestIpAddress(MOCK_IP_ADDRESS).withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB");
        assertThat(aCase.testDescription, runCase(aCase), is(true));
    }

    @Test
    public void testSingleCountryEvaluatesFalseIfIpResolvesToIsoCodeNotInValuesForIs() throws Exception {
        CountryConditionletCase aCase = new CountryConditionletCase("If Ip resolves to GB and GB is not values, evaluate to false.", "is");
        aCase.withIsoCode("US").withRequestIpAddress(MOCK_IP_ADDRESS).withMockIpToIsoCode(MOCK_IP_ADDRESS, "GB");
        assertThat(aCase.testDescription, runCase(aCase), is(false));
    }

    @Test
    public void testSingleCountryEvaluatesFalseIfIpCannotBeResolved() throws Exception {
        CountryConditionletCase aCase = new CountryConditionletCase("If Ip does not resolve, evaluate to false.", "is");
        aCase.withIsoCode("US").withRequestIpAddress(MOCK_IP_ADDRESS);
        when(geoIp2Util.getCountryIsoCode(MOCK_IP_ADDRESS)).thenThrow(new GeoIp2Exception("Boom."));
        assertThat(aCase.testDescription, runCase(aCase), is(false));
    }

    private boolean runCase(CountryConditionletCase aCase) {
        return conditionlet.evaluate(aCase.request, aCase.response, aCase.comparisonId, aCase.values);
    }

    private class CountryConditionletCase {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final String comparisonId;
        private final List<ConditionValue> values;
        private final String testDescription;

        public CountryConditionletCase(String testDescription, String comparisonId) {
            this(testDescription, comparisonId, Lists.<ConditionValue>newArrayList());
        }

        public CountryConditionletCase(String testDescription, String comparisonId, List<ConditionValue> values) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
            this.comparisonId = comparisonId;
            this.values = values;
        }

        CountryConditionletCase withRequestIpAddress(String mockIpAddress) throws IOException, GeoIp2Exception {
            when(request.getHeader("X-Forwarded-For")).thenReturn(mockIpAddress);
            return this;
        }

        CountryConditionletCase withMockIpToIsoCode(String mockIpAddress, String mockIsoCode) throws IOException, GeoIp2Exception {
            when(geoIp2Util.getCountryIsoCode(mockIpAddress)).thenReturn(mockIsoCode);
            return this;
        }

        CountryConditionletCase withIsoCode(String isoCode) {
            ConditionValue value = new ConditionValue();
            value.setKey("isoCode");
            value.setValue(isoCode);
            values.add(value);
            return this;
        }
    }
}