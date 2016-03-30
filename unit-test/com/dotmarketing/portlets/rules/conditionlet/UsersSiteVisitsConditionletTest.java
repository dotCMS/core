package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionlet.SITE_VISITS_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN_OR_EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN_OR_EQUAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by freddyrodriguez on 10/3/16.
 */
public class UsersSiteVisitsConditionletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession httpSessionMock;
    private UsersSiteVisitsConditionlet conditionlet = new UsersSiteVisitsConditionlet();


    @DataProvider(name = "cases")
    public Object[][] compareCases() throws Exception {
        try {
            List<TestCase> data = Lists.newArrayList();


            /* Equal */
            data.add(new TestCase("True when equal.")
                         .withComparison(EQUAL)
                         .withActualVisitCount(1)
                         .withCountParameterValue(1)
                         .shouldBeTrue()
            );

            data.add(new TestCase("False when not equal")
                         .withComparison(EQUAL)
                         .withActualVisitCount(0)
                         .withCountParameterValue(1)
                         .shouldBeFalse()
            );

             /* Greater than */
            data.add(new TestCase("True when greater than.")
                         .withComparison(GREATER_THAN)
                         .withActualVisitCount(10)
                         .withCountParameterValue(2)
                         .shouldBeTrue()
            );

            data.add(new TestCase("False when not greater than")
                         .withComparison(GREATER_THAN)
                         .withActualVisitCount(10)
                         .withCountParameterValue(10)
                         .shouldBeFalse()
            );

             /* Less than */
            data.add(new TestCase("True when less than.")
                         .withComparison(LESS_THAN)
                         .withActualVisitCount(9)
                         .withCountParameterValue(10)
                         .shouldBeTrue()
            );

            data.add(new TestCase("False when not less than")
                         .withComparison(LESS_THAN)
                         .withActualVisitCount(10)
                         .withCountParameterValue(10)
                         .shouldBeFalse()
            );

            /* Less than or equal */
            data.add(new TestCase("True when less than.")
                         .withComparison(LESS_THAN_OR_EQUAL)
                         .withActualVisitCount(9)
                         .withCountParameterValue(10)
                         .shouldBeTrue()
            );

            data.add(new TestCase("True when equal.")
                         .withComparison(LESS_THAN_OR_EQUAL)
                         .withActualVisitCount(10)
                         .withCountParameterValue(10)
                         .shouldBeTrue()
            );

            data.add(new TestCase("False when not less than")
                         .withComparison(LESS_THAN_OR_EQUAL)
                         .withActualVisitCount(11)
                         .withCountParameterValue(10)
                         .shouldBeFalse()
            );

             /* Greater than or equal */
            data.add(new TestCase("True when greater than.")
                         .withComparison(GREATER_THAN_OR_EQUAL)
                         .withActualVisitCount(12)
                         .withCountParameterValue(10)
                         .shouldBeTrue()
            );

            data.add(new TestCase("True when equal.")
                         .withComparison(GREATER_THAN_OR_EQUAL)
                         .withActualVisitCount(10)
                         .withCountParameterValue(10)
                         .shouldBeTrue()
            );

            data.add(new TestCase("False when not greater than")
                         .withComparison(GREATER_THAN_OR_EQUAL)
                         .withActualVisitCount(9)
                         .withCountParameterValue(10)
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
        assertThat(aCase.toString(), runCase(aCase), is(aCase.expect));
    }

    private boolean runCase(TestCase aCase) {
        return aCase.conditionlet.evaluate(aCase.request, aCase.response, aCase.conditionlet.instanceFrom(aCase.params));
    }

    private class TestCase {

        public final UsersSiteVisitsConditionlet conditionlet;
        public final Clickstream clickstream = mock(Clickstream.class);
        private final HttpServletRequest request = mock(HttpServletRequest.class);
        private final HttpSession session = mock(HttpSession.class);
        private final HttpServletResponse response = mock(HttpServletResponse.class);
        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();

        private boolean expect;
        private String siteVisitCount;
        private int actualVisitsCount;
        private Comparison comparison;

        public TestCase() {
            this("");
        }
        public TestCase(String description) {
            conditionlet = new UsersSiteVisitsConditionlet();
            when(request.getSession(true)).thenReturn(session);
            when(session.getAttribute("clickstream")).thenReturn(clickstream);
        }

        TestCase shouldBeTrue() {
            this.expect = true;
            return this;
        }

        TestCase shouldBeFalse() {
            this.expect = false;
            return this;
        }

        TestCase withComparison(Comparison comparison) {
            this.comparison = comparison;
            params.put(COMPARISON_KEY, new ParameterModel(COMPARISON_KEY, comparison != null ? comparison.getId() : null));
            return this;
        }

        public TestCase withActualVisitCount(int actualVisitsCount) {
            this.actualVisitsCount = actualVisitsCount;
            when(clickstream.getNumberOfRequests()).thenReturn(actualVisitsCount);
            return this;
        }

        public TestCase withCountParameterValue(int count) {
            return this.withCountParameterValue(String.valueOf(count));
        }

        public TestCase withCountParameterValue(String count) {
            this.siteVisitCount = count;
            params.put(SITE_VISITS_KEY, new ParameterModel(SITE_VISITS_KEY, count));
            return this;
        }

        @Override
        public String toString() {
            return String.format("Expect %s for '%s' '%s' '%s' ['actualVisits' 'comparison' 'siteVisitParameter']",
                                 expect, actualVisitsCount, comparison != null ? comparison.getId() : null, siteVisitCount );
        }


    }

}
