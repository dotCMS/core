package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.ReferringURLConditionlet.REFERRING_URL_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;

public class UsersCurrentURLConditionletTest {

    @DataProvider(name = "cases")
    public Object[][] compareCases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


        /* Is */
            data.add(new TestCase("If /products/ is set and the current URL is /products/, evaluate to true.")
                         .withComparison(IS)
                         .withReferrer("/products/")
                         .withMockReferrer("/products/")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Ignores case - If /Products/ is set and current URL is /products/ , evaluate to true.")
                         .withComparison(IS)
                         .withReferrer("/Products/")
                         .withMockReferrer("/products/")
                         .shouldBeTrue()
            );

            data.add(new TestCase("If /products/ set and current URL is /about-us/ , evaluate to false.")
                         .withComparison(IS)
                         .withReferrer("/products/")
                         .withMockReferrer("/about-us/")
                         .shouldBeFalse()
            );

            /* Is Not*/
            data.add(new TestCase("Is Not: If /products/ set and current URL is /about-us/ , evaluate to true.")
                        .withComparison(IS_NOT)
                        .withReferrer("/products/")
                        .withMockReferrer("/about-us/")
                        .shouldBeTrue()
            );

            data.add(new TestCase("Is Not: If /products/ set and current URL is /products/ , evaluate to false.")
                         .withComparison(IS_NOT)
                         .withReferrer("/products/")
                         .withMockReferrer("/products/")
                         .shouldBeFalse()
            );

            data.add(new TestCase("Starts with: If /prod set and current URL is /products/ , evaluate to true.")
                    .withComparison(STARTS_WITH)
                    .withReferrer("/prod")
                    .withMockReferrer("/products/")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Starts with: If /prod set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(STARTS_WITH)
                    .withReferrer("/prod")
                    .withMockReferrer("/about-us/")
                    .shouldBeFalse()
            );

            data.add(new TestCase("Ends with: If ducts/ set and current URL is /products/ , evaluate to true.")
                    .withComparison(ENDS_WITH)
                    .withReferrer("ducts/")
                    .withMockReferrer("/products/")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Ends with: If ducts/ set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(ENDS_WITH)
                    .withReferrer("ducts/")
                    .withMockReferrer("/about-us/")
                    .shouldBeFalse()
            );
            data.add(new TestCase("Contains: If product set and current URL is /products/ , evaluate to true.")
                    .withComparison(CONTAINS)
                    .withReferrer("product")
                    .withMockReferrer("/products/")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Contains: If product set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(CONTAINS)
                    .withReferrer("product")
                    .withMockReferrer("/about-us/")
                    .shouldBeFalse()
            );
            data.add(new TestCase("Regexp: If \\/pro.* set and current URL is /products/ , evaluate to true.")
                    .withComparison(REGEX)
                    .withReferrer("\\/pro.*")
                    .withMockReferrer("/products/")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Regexp: If \\/pro.* set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(REGEX)
                    .withReferrer("\\/pro.*")
                    .withMockReferrer("/about-us/")
                    .shouldBeFalse()
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

    private class TestCase {

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

        TestCase withMockReferrer(String mockReferrer) throws IOException{
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
