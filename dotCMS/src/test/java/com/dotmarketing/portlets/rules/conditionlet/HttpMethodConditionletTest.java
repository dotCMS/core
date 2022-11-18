package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.HttpMethodConditionlet.HTTP_METHOD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Test for the {@link HttpMethodConditionlet} class.
 *
 * @author Jose Castro
 * @since Nov 15th, 2022
 */
@RunWith(DataProviderRunner.class)
public class HttpMethodConditionletTest extends UnitTestBase {

    private HttpMethodConditionlet conditionlet;

    @Before
    public void setUp() throws Exception {
        this.conditionlet = new HttpMethodConditionlet();
    }

    /**
     * Makes sure that the following assertions are correct:
     * <ul>
     *     <li>Comparison 'IS' should eval 'true' for HTTP Method value 'GET' and user value 'GET'.</li>
     *     <li>Comparison 'IS' should eval 'false' for HTTP Method value 'GET' and user value 'POST'.</li>
     *     <li>Comparison 'IS_NOT' should eval 'true' for HTTP Method value 'GET' and user value 'DELETE'.</li>
     *     <li>Comparison 'IS_NOT' should eval 'false' for HTTP Method value 'PATCH' and user value 'PATCH'.</li>
     * </ul>
     */
    @DataProvider
    public static Object[][] cases() {
        final List<TestCase> data = Lists.newArrayList();
        data.add(new TestCase("Comparison 'IS' should eval 'true' for HTTP Method value 'GET' and user value 'GET'")
                         .withComparison(Comparison.IS)
                         .withMockedActualValue("GET")
                         .withParameter("GET")
                         .shouldBeTrue()
        );
        data.add(new TestCase("Comparison 'IS' should eval 'false' for HTTP Method value 'GET' and user value 'POST'")
                         .withComparison(Comparison.IS)
                         .withMockedActualValue("GET")
                         .withParameter("POST")
                         .shouldBeFalse()
        );

        data.add(new TestCase("Comparison 'IS_NOT' should eval 'true' for HTTP Method value 'GET' and user value 'DELETE'")
                         .withComparison(Comparison.IS_NOT)
                         .withMockedActualValue("GET")
                         .withParameter("DELETE")
                         .shouldBeTrue()
        );
        data.add(new TestCase("Comparison 'IS_NOT' should eval 'false' for HTTP Method value 'PATCH' and user value 'PATCH'")
                         .withComparison(Comparison.IS_NOT)
                         .withMockedActualValue("PATCH")
                         .withParameter("PATCH")
                         .shouldBeFalse()
        );
        return TestUtil.toCaseArray(data);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link HttpMethodConditionlet#getI18nKey()} and
     *     {@link HttpMethodConditionlet#getParameterDefinitions()}</li>
     *     <li><b>Given Scenario:</b> Making sure that the Conditionlet's definition uses the expected i18n key and has
     *     the expected number of input parameters.</li>
     *     <li><b>Expected Result:</b> All assertions in the test must be true.</li>
     * </ul>
     */
    @Test
    public void testGeneralConfiguration() {
        assertThat(this.conditionlet.getI18nKey(), is("api.ruleengine.system.conditionlet.HttpMethodConditionlet"));
        assertThat("HttpMethodConditionlet must have two input parameters.",
                this.conditionlet.getParameterDefinitions().size(), is(2));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     * {@link HttpMethodConditionlet#evaluate(HttpServletRequest, HttpServletResponse, HttpMethodConditionlet.Instance)}</li>
     *     <li><b>Given Scenario:</b> Test Cases defined by the {@link #cases()} method.</li>
     *     <li><b>Expected Result:</b> All assertions in the test must be true.</li>
     * </ul>
     */
    @Test
    @UseDataProvider("cases")
    public void testComparisons(final TestCase testCase) throws Exception {
        assertThat(testCase.testDescription, runCase(testCase), is(testCase.expect));
    }

    /**
     * Runs the specified Test Case and evaluates it as required by this specific Integration Test.
     *
     * @param testCase The {@link TestCase}.
     *
     * @return The result of evaluating the test.
     */
    private boolean runCase(final TestCase testCase) {
        return this.conditionlet.evaluate(testCase.request, testCase.response,
                this.conditionlet.instanceFrom(testCase.params));
    }

    /**
     * Base class used for defining the different input parameters required by the tests.
     */
    private static class TestCase {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;
        private boolean expect;

        public TestCase(final String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
        }

        TestCase shouldBeTrue() {
            this.expect = true;
            return this;
        }

        TestCase shouldBeFalse() {
            this.expect = false;
            return this;
        }

        TestCase withComparison(final Comparison<Object> comparison) {
            params.put(COMPARISON_KEY, new ParameterModel(COMPARISON_KEY, comparison != null ? comparison.getId() :
                                                                                  null));
            return this;
        }

        TestCase withParameter(final String value) {
            params.put(HTTP_METHOD, new ParameterModel(HTTP_METHOD, value));
            return this;
        }

        TestCase withMockedActualValue(final String value) {
            when(this.request.getMethod()).thenReturn(value);
            return this;
        }

        @Override
        public String toString() {
            return this.testDescription;
        }

    }

}
