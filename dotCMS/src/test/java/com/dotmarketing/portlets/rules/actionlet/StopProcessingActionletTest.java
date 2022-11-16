package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.dotmarketing.portlets.rules.actionlet.StopProcessingActionlet.RETURN_CODE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Test for the {@link StopProcessingActionlet} class.
 *
 * @author Jose Castro
 * @since Nov 15th, 2022
 */
@RunWith(DataProviderRunner.class)
public class StopProcessingActionletTest extends UnitTestBase {

    private StopProcessingActionlet actionlet;

    @Before
    public void setUp() throws Exception {
        this.actionlet = new StopProcessingActionlet();
    }

    /**
     * Makes sure that the following assertions are correct:
     * <ul>
     *     <li>Not passing down a value for this actionlet must default to "200", match the status in the HTTP
     *     Response, and evaluation must be 'true' as long as the HTTP response is NOT committed.</li>
     *     <li>Passing down any value for this actionlet must match the status in the HTTP Response, and evaluation
     *     must be 'true' as long as the HTTP response is NOT committed.</li>
     *     <li>Passing down any value for this actionlet must evaluate to 'false' because the HTTP response IS
     *     committed.</li>
     * </ul>
     */
    @DataProvider
    public static Object[][] cases() {
        return new TestCase[][]{
                {
                    new TestCase("An empty HTTP Status Code defaults to 200")
                         .withParameter(StringPool.BLANK).isCommitted(false).shouldBeTrue()},
                {
                    new TestCase("Set HTTP Status Code to 403")
                         .withParameter("403").isCommitted(false).shouldBeTrue()},
                {
                    new TestCase("A committed response must always return 'false'")
                         .withParameter("301").isCommitted(true).shouldBeFalse()}
        };
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link StopProcessingActionlet#getI18nKey()} and
     *     {@link StopProcessingActionlet#getParameterDefinitions()}</li>
     *     <li><b>Given Scenario:</b> Making sure that the Actionlet's definition uses the expected i18n key and has
     *     the expected number of input parameters.</li>
     *     <li><b>Expected Result:</b> All assertions in the test must be true.</li>
     * </ul>
     */
    @Test
    public void testGeneralConfiguration() {
        assertThat(actionlet.getI18nKey(), is("api.system.ruleengine.actionlet.StopProcessingActionlet"));
        assertThat("StopProcessingActionlet must have one inputparameter.",
                actionlet.getParameterDefinitions().size(), is(1));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     * {@link StopProcessingActionlet#evaluate(HttpServletRequest, HttpServletResponse, StopProcessingActionlet.Instance)}</li>
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
        final boolean evaluation = this.actionlet.evaluate(testCase.request, testCase.response,
                this.actionlet.instanceFrom(testCase.params));
        if (!testCase.response.isCommitted()) {
            return evaluation;
        } else {
            return evaluation && testCase.response.getStatus() == Integer.parseInt(testCase.params.get(RETURN_CODE).toString());
        }
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

        TestCase withParameter(final String value) {
            params.put(RETURN_CODE, new ParameterModel(RETURN_CODE, value));
            return this;
        }

        TestCase isCommitted(final boolean value) {
            when(this.response.isCommitted()).thenReturn(value);
            return this;
        }

        @Override
        public String toString() {
            return this.testDescription;
        }

    }

}
