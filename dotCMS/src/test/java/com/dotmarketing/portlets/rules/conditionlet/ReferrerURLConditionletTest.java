package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.ReferringURLConditionlet.REFERRING_URL_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EXISTS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ReferrerURLConditionletTest extends UnitTestBase {

    @DataProvider
    public static Object[][] cases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


        /* Is */
            data.add(new TestCase("If google.com set and visitor's referrer URL is google , evaluate to true.")
                         .withComparison(IS)
                         .withReferrer("google.com")
                         .withMockReferrer("google.com")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Ignores case - If google.com set and visitor's referrer URL is google.COM , evaluate to true.")
                         .withComparison(IS)
                         .withReferrer("google.com")
                         .withMockReferrer("google.COM")
                         .shouldBeTrue()
            );

            data.add(new TestCase("If google.com set and visitor's referring URL is yahoo.com , evaluate to false.")
                         .withComparison(IS)
                         .withReferrer("google.com")
                         .withMockReferrer("yahoo.com")
                         .shouldBeFalse()
            );

            /* Is Not*/
            data.add(new TestCase("Is Not: If google.com set and visitor's referring URL is yahoo.com , evaluate to true.")
                        .withComparison(IS_NOT)
                        .withReferrer("google.com")
                        .withMockReferrer("yahoo.com")
                        .shouldBeTrue()
            );

            data.add(new TestCase("Is Not: If google.com set and visitor's referrer URL is google , evaluate to false.")
                         .withComparison(IS_NOT)
                         .withReferrer("google.com")
                         .withMockReferrer("google.com")
                         .shouldBeFalse()
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
            .withReferrer("google.com")
            .withMockReferrer("google.com")
            .shouldBeFalse();
        runCase(aCase);
    }

    private static class TestCase {

        public final ReferringURLConditionlet conditionlet;

        private final HttpServletRequest request ;
        private final HttpServletResponse response;

        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;

        private boolean expect;

        public TestCase(String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
            conditionlet = new ReferringURLConditionlet();
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

        TestCase withMockReferrer(String mockReferrer) throws IOException, GeoIp2Exception {
            when(request.getHeader("referer")).thenReturn(mockReferrer);
            return this;
        }

        TestCase withReferrer(String referrer) {
            params.put(REFERRING_URL_KEY, new ParameterModel(REFERRING_URL_KEY, referrer));
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
