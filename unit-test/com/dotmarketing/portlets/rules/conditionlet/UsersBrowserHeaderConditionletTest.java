package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.UsersBrowserHeaderConditionlet.HEADER_NAME_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.UsersBrowserHeaderConditionlet.HEADER_VALUE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsersBrowserHeaderConditionletTest {

    private UsersBrowserHeaderConditionlet conditionlet;

    @BeforeMethod
    public void setUp() throws Exception {
        conditionlet = new UsersBrowserHeaderConditionlet();
    }


    @DataProvider(name = "cases")
    public Object[][] compareCases() {
        List<TestCase> data = Lists.newArrayList();

        data.add(new TestCase("Comparison 'Exists' should eval true for header value == '' and user value == ''.")
                 .withComparison(Comparison.EXISTS)
                 .withMockedActualValue("key", "")
                 .withHeader("key", "")
                 .shouldBeTrue()
        );
        data.add(new TestCase("Comparison 'Exists' should eval false for header value == null and user value == ''.")
                     .withComparison(Comparison.EXISTS)
                     .withMockedActualValue("key", null)
                     .withHeader("key", "")
                     .shouldBeFalse()

        );
        /* Is */
        data.add(new TestCase("Comparison 'Is' should eval true for header value == '' and user value == ''.")
                     .withComparison(Comparison.IS)
                     .withMockedActualValue("key", "")
                     .withHeader("key", "")
                     .shouldBeTrue()
        );

        data.add(new TestCase("Comparison 'Is' should eval true for header value == '1' and user value == '1'.")
                     .withComparison(Comparison.IS)
                     .withMockedActualValue("key", "")
                     .withHeader("key", "")
                     .shouldBeTrue()
        );

        data.add(new TestCase("Comparison 'Is' should eval false for header value == '1' and user value == ''.")
                     .withComparison(Comparison.IS)
                     .withMockedActualValue("key", "1")
                     .withHeader("key", "")
                     .shouldBeFalse()
        );

        data.add(new TestCase("Comparison 'Is' should not be case sensitive - should eval true for header value == 'One' and user value == 'one'.")
                     .withComparison(Comparison.IS)
                     .withMockedActualValue("key", "One")
                     .withHeader("key", "one")
                     .shouldBeTrue()
        );
        /* Is Not */
        data.add(new TestCase("Comparison 'Is Not' should eval false for header value == '' and user value == ''.")
                     .withComparison(Comparison.IS_NOT)
                     .withMockedActualValue("key", "")
                     .withHeader("key", "")
                     .shouldBeFalse()
        );

        data.add(new TestCase("Comparison 'Is Not' should eval false for header value == '1' and user value == '1'.")
                     .withComparison(Comparison.IS_NOT)
                     .withMockedActualValue("key", "")
                     .withHeader("key", "")
                     .shouldBeFalse()
        );

        data.add(new TestCase("Comparison 'Is Not' should eval true for header value == '1' and user value == ''.")
                     .withComparison(Comparison.IS_NOT)
                     .withMockedActualValue("key", "1")
                     .withHeader("key", "")
                     .shouldBeTrue()
        );

        data.add(new TestCase("Comparison 'Is Not' should not be case sensitive - should eval false for header value == 'One' and user value == 'one'.")
                     .withComparison(Comparison.IS_NOT)
                     .withMockedActualValue("key", "One")
                     .withHeader("key", "one")
                     .shouldBeFalse()
        );
        /* Starts With  */
        String mockedActual;
        String userValue;
        String description;
        mockedActual = "The Quick Brown Fox";
        userValue = "the quick";
        description = String.format("Comparison 'Starts With' should eval true for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.STARTS_WITH)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeTrue()
        );
        mockedActual = "The Quick Brown Fox";
        userValue = "A quick";
        description = String.format("Comparison 'Starts With' should eval false for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.STARTS_WITH)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeFalse()
        );
        /* Ends With */
        mockedActual = "The Quick Brown Fox";
        userValue = "Brown fox";
        description = String.format("Comparison 'Ends With' should eval true for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.ENDS_WITH)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeTrue()
        );
        mockedActual = "The Quick Brown Fox";
        userValue = "foxy";
        description = String.format("Comparison 'Ends With' should eval false for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.ENDS_WITH)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeFalse()
        );
        /* Contains */
        mockedActual = "The Quick Brown Fox";
        userValue = "quick brown";
        description = String.format("Comparison 'Contains' should eval true for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.CONTAINS)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeTrue()
        );
        mockedActual = "The Quick Brown Fox";
        userValue = "bob";
        description = String.format("Comparison 'Ends With' should eval false for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.CONTAINS)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeFalse()
        );
        /* Matches Regex */
        mockedActual = "The Quick Brown Fox";
        userValue = ".*Quick.*Fox";
        description = String.format("Comparison 'Regex' should eval true for header value == '%s' and user value == '%s'.", mockedActual, userValue);
        data.add(new TestCase(description)
                     .withComparison(Comparison.REGEX)
                     .withMockedActualValue("key", mockedActual)
                     .withHeader("key", userValue)
                     .shouldBeTrue()
        );




        return TestUtil.toCaseArray(data);
    }

    @Test(dataProvider = "cases")
    public void testComparisons(TestCase aCase) throws Exception {
        assertThat(aCase.testDescription, runCase(aCase), is(aCase.expect));
    }

    private boolean runCase(TestCase aCase) {
        return conditionlet.evaluate(aCase.request, aCase.response, conditionlet.instanceFrom(aCase.params));
    }

    private class TestCase {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;
        private boolean expect;


        public TestCase(String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
        }

        TestCase shouldBeTrue(){
            this.expect = true;
            return this;
        }

        TestCase shouldBeFalse() {
            this.expect = false;
            return this;
        }

        TestCase withComparison(Comparison c){
            params.put(COMPARISON_KEY, new ParameterModel(COMPARISON_KEY, c != null ? c.getId() : null));
            return this;
        }

        TestCase withHeader(String key, String value) {
            params.put(HEADER_NAME_KEY, new ParameterModel(HEADER_NAME_KEY, key));
            params.put(HEADER_VALUE_KEY, new ParameterModel(HEADER_VALUE_KEY, value));
            return this;
        }

        TestCase withMockedActualValue(String key, String value){
            when(this.request.getHeader(key)).thenReturn(value);
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}