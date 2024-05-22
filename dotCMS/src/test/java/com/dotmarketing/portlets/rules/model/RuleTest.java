package com.dotmarketing.portlets.rules.model;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.unittest.TestUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class RuleTest extends UnitTestBase {

    @DataProvider
    public static Object[][] cases() {
        try {

            List<TestCase> data = Lists.newArrayList();

            // A || B && C ==> A || (B && C) vs (A || B) && C
            //

            data.add(new TestCase("Evaluates to true if no groups.")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Evaluates to true if one 'AND' group that itself is true.")
                         .withGroup(LogicalOperator.AND, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("Evaluates to false if one 'AND' group that itself is false.")
                         .withGroup(LogicalOperator.AND, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("Evaluates to true if one 'OR' group that itself is true.")
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("Evaluates to false if one 'OR' group that itself is false.")
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("(true && true) should be true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.AND, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("(true && false) should be false.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.AND, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("(true || false) should be true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("(false || true) should be true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("(false || false) should be false.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( true && true ) ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, true)
                         .shouldBeTrue()
            );
            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( true && false ) ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( false && true ) ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( false && false ) ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (false || ( true && true ) ) ==> true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, true)
                         .shouldBeTrue()
            );

            // four terms
            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && true ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && true ) || false ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && false ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && false ) || false ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeTrue()
            );

            //
            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && true ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && true ) || false ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && false ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && false ) || false ) ==> true.")
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeTrue()
            );

            //
            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && true ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && true ) || false ) ==> true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && false ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && false ) || false ) ==> false.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && true ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && true ) || false ) ==> false.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, true)
                         .withGroup(LogicalOperator.OR, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && false ) || true ) ==> true.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && false ) || false ) ==> false.")
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
                         .withGroup(LogicalOperator.AND, false)
                         .withGroup(LogicalOperator.OR, false)
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
        Rule rule = new Rule();
        return rule.evaluateConditions(aCase.request, aCase.response, aCase.groups);
    }

    private static class TestCase {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final List<ConditionGroup> groups = Lists.newArrayList();
        private final String testDescription;
        private boolean expect;

        public TestCase(String testDescription) {
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

        TestCase withGroup(LogicalOperator op, boolean result) {
            ConditionGroup g = mock(ConditionGroup.class);
            when(g.getOperator()).thenReturn(op);
            when(g.evaluate(Mockito.eq(request), Mockito.eq(response), Mockito.anyList())).thenReturn(result);
            groups.add(g);
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}