package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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


    @DataProvider(name = "compareCases")
    public Object[][] compareCases() {
        List<TestCase> data = Lists.newArrayList();

        data.add(new TestCase("Comparison 'Exists' should eval true for header value == '' and user value == ''.")
                 .withComparison(Comparison.EXISTS)
                 .withMockedHeader("key", "")
                 .withHeader("key", "")
                 .shouldBeTrue()
        );
        data.add(new TestCase("Comparison 'Exists' should eval false for header value == null and user value == ''.")
                     .withComparison(Comparison.EXISTS)
                     .withMockedHeader("key", null)
                     .withHeader("key", "")
                     .shouldBeFalse()

        );
        data.add(new TestCase("Comparison 'Is' should eval true for header value == '' and user value == ''.")
                     .withComparison(Comparison.IS)
                     .withMockedHeader("key", "")
                     .withHeader("key", "")
                     .shouldBeTrue()
        );

        data.add(new TestCase("Comparison 'Is' should eval true for header value == '1' and user value == '1'.")
                     .withComparison(Comparison.IS)
                     .withMockedHeader("key", "")
                     .withHeader("key", "")
                     .shouldBeTrue()
        );

        data.add(new TestCase("Comparison 'Is' should eval false for header value == '1' and user value == ''.")
                     .withComparison(Comparison.IS)
                     .withMockedHeader("key", "1")
                     .withHeader("key", "")
                     .shouldBeFalse()
        );

        data.add(new TestCase("Comparison 'Is' should not be case sensitive - should eval true for header value == 'One' and user value == 'one'.")
                     .withComparison(Comparison.IS)
                     .withMockedHeader("key", "One")
                     .withHeader("key", "one")
                     .shouldBeTrue()
        );

        return TestUtil.toCaseArray(data);
    }

    @Test(dataProvider = "compareCases")
    public void testComparisons(TestCase aCase) throws Exception {
        assertThat(aCase.testDescription, runCase(aCase), is(aCase.expect));
    }

    private boolean runCase(TestCase aCase) {
        return conditionlet.evaluate(aCase.request, aCase.response, conditionlet.instanceFrom(aCase.comparison, aCase.values));
    }

    private class TestCase {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final List<ParameterModel> values = Lists.newArrayList();
        private final String testDescription;
        private Comparison comparison;
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
            this.comparison = c;
            return this;
        }

        TestCase withHeaderKey(String keyValue) {
            ParameterModel value = new ParameterModel(HEADER_NAME_KEY, keyValue);
            values.add(value);
            return this;
        }

        TestCase withHeader(String key, String value) {
            values.add(new ParameterModel(HEADER_NAME_KEY, key));
            values.add(new ParameterModel(HEADER_VALUE_KEY, value));
            return this;
        }

        TestCase withMockedHeader(String key, String value){
            when(this.request.getHeader(key)).thenReturn(value);
            return this;
        }


    }
}