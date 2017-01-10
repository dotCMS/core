package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.unittest.TestUtil;
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
import static com.dotmarketing.portlets.rules.conditionlet.VisitorsCurrentUrlConditionlet.PATTERN_URL_INPUT_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class VisitorsCurrentURLConditionletTest {

    @DataProvider
    public static Object[][] cases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


        /* Is */
            data.add(new TestCase("If /products/ is set and the current URL is /products/, evaluate to true.")
                         .withComparison(IS)
                         .withURI("/products/index")
                         .withPattern("/products/index")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Case sencitive- If /Products/ is set and current URL is /products/ , evaluate to false.")
                         .withComparison(IS)
                         .withURI("/products/index")
                         .withPattern("/Products/index")
                         .shouldBeFalse()
            );

            data.add(new TestCase("If /products/ set and current URL is /about-us/ , evaluate to false.")
                         .withComparison(IS)
                         .withURI("/about-us/")
                         .withPattern("/products/index")
                         .shouldBeFalse()
            );

            /* Is Not*/
            data.add(new TestCase("Is Not: If /products/ set and current URL is /about-us/ , evaluate to true.")
                        .withComparison(IS_NOT)
                        .withURI("/about-us/")
                        .withPattern("/products/index")
                        .shouldBeTrue()
            );

            data.add(new TestCase("Is Not: If /products/ set and current URL is /products/ , evaluate to false.")
                         .withComparison(IS_NOT)
                         .withURI("/products/index")
                         .withPattern("/products/index")
                         .shouldBeFalse()
            );

            data.add(new TestCase("Starts with: If /prod set and current URL is /products/ , evaluate to true.")
                    .withComparison(STARTS_WITH)
                    .withURI("/products/index")
                    .withPattern("/prod")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Starts with: If /prod set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(STARTS_WITH)
                    .withURI("/about-us/index")
                    .withPattern("/prod")
                    .shouldBeFalse()
            );

            data.add(new TestCase("Ends with: If ducts/ set and current URL is /products/ , evaluate to true.")
                    .withComparison(ENDS_WITH)
                    .withURI("/products/index")
                    .withPattern("ducts/index")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Ends with: If ducts/ set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(ENDS_WITH)
                    .withURI("/about-us/index")
                    .withPattern("ducts/index")
                    .shouldBeFalse()
            );
            data.add(new TestCase("Contains: If product set and current URL is /products/ , evaluate to true.")
                    .withComparison(CONTAINS)
                    .withURI("/products/index")
                    .withPattern("product")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Contains: If product set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(CONTAINS)
                    .withURI("/about-us/index")
                    .withPattern("product")
                    .shouldBeFalse()
            );
            data.add(new TestCase("Regexp: If \\/pro.* set and current URL is /products/ , evaluate to true.")
                    .withComparison(REGEX)
                    .withURI("/products/index")
                    .withPattern("\\/pro.*")
                    .shouldBeTrue()
            );

            data.add(new TestCase("Regexp: If \\/pro.* set and current URL is /about-us/ , evaluate to true.")
                    .withComparison(REGEX)
                    .withURI("/about-us/index")
                    .withPattern("\\/pro.*")
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
        return aCase.conditionlet.evaluate(aCase.request, aCase.conditionlet.instanceFrom(aCase.params), aCase.uri,aCase.pattern);
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

    private static class TestCase {

        public final VisitorsCurrentUrlConditionlet conditionlet;

        private final HttpServletRequest request ;
        private final HttpServletResponse response;
        private String uri;
        private String pattern;
        private String index;

        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;

        private boolean expect;

        public TestCase(String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
            conditionlet = new VisitorsCurrentUrlConditionlet();
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

        TestCase withPattern(String mockReferrer) throws IOException{
        	params.put(PATTERN_URL_INPUT_KEY, new ParameterModel(PATTERN_URL_INPUT_KEY, mockReferrer));
        	pattern = mockReferrer;
            return this;
        }

        TestCase withURI(String referrer) {
            uri = referrer;
            return this;
        }

        TestCase withIndex(String index) {
        	this.index = index;
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
