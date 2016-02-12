package com.dotmarketing.portlets.rules.util;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.dotmarketing.portlets.rules.model.LogicalOperator.AND;
import static com.dotmarketing.portlets.rules.model.LogicalOperator.OR;
import static com.dotmarketing.portlets.rules.util.LogicalStatement.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class LogicalStatementTest {

    @DataProvider(name = "cases")
    public Object[][] compareCases() {

        List<TestCase> data = Lists.newArrayList();

        // A || B && C ==> A || (B && C) vs (A || B) && C
        //

        data.add(new TestCase("Evaluates to true if no conditions.")
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( false && true ) ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .withTerm(AND, true)
                     .shouldBeTrue()
        );

        // four terms
        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && true ) || true ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && true ) || false ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && false ) || true ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( true && false ) || false ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .shouldBeTrue()
        );

        //
        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && true ) || true ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && true ) || false ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && false ) || true ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (true || ( false && false ) || false ) ==> true.")
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .shouldBeTrue()
        );

        //
        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && true ) || true ) ==> true.")
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && true ) || false ) ==> true.")
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && false ) || true ) ==> true.")
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( true && false ) || false ) ==> false.")
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .shouldBeFalse()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && true ) || true ) ==> true.")
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .withTerm(AND, true)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && true ) || false ) ==> false.")
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .withTerm(AND, true)
                     .withTerm(OR, false)
                     .shouldBeFalse()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && false ) || true ) ==> true.")
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        data.add(new TestCase("A || B && C || D should evaluate as ( A || ( B && C) || D ):  (false || ( false && false ) || false ) ==> false.")
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .shouldBeFalse()
        );

        data.add(new TestCase("A || B && C || D && E && F || G:  (false || ( true && false ) || (false && true && false) || true ) ==> true.")
                     .withTerm(false)
                     .withTerm(OR, true)
                     .withTerm(AND, false)
                     .withTerm(OR, false)
                     .withTerm(AND, true)
                     .withTerm(AND, false)
                     .withTerm(OR, true)
                     .shouldBeTrue()
        );

        return TestUtil.toCaseArray(data);
    }

    @Test(dataProvider = "cases", invocationCount = 1)
    public void testComparisons(TestCase aCase) throws Exception {
        assertThat(aCase.testDescription, runCase(aCase), is(aCase.expect));
    }

    private boolean runCase(TestCase aCase) {
        return aCase.statement.evaluate();
    }

    private class TestCase {

        private final LogicalStatement statement = new LogicalStatement();
        private final String testDescription;
        private boolean expect;

        public TestCase(String testDescription) {
            this.testDescription = testDescription;
        }

        TestCase shouldBeTrue() {
            this.expect = true;
            return this;
        }

        TestCase shouldBeFalse() {
            this.expect = false;
            return this;
        }

        TestCase withTerm(boolean result) {
            return this.withTerm(AND, result);
        }
        TestCase withTerm(LogicalOperator op, boolean result) {
            statement.addTerm(op, new BooleanCondition(result));
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}