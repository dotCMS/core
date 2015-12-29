package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.unittest.TestUtil;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleTest {

    @DataProvider(name = "cases")
    public Object[][] compareCases() {
        try {

            List<TestCase> data = Lists.newArrayList();

            data.add(new TestCase("Evaluates to true if no groups.")
                         .shouldBeTrue()
            );

            data.add(new TestCase("Evaluates to true if one 'AND' group that itself is true.")
                         .withGroup(Condition.Operator.AND, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("Evaluates to false if one 'AND' group that itself is false.")
                         .withGroup(Condition.Operator.AND, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("Evaluates to true if one 'OR' group that itself is true.")
                         .withGroup(Condition.Operator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("Evaluates to false if one 'OR' group that itself is false.")
                         .withGroup(Condition.Operator.OR, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("(true && true) should be true.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.AND, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("(true && false) should be false.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.AND, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("(true || false) should be true.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.OR, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("(false || true) should be true.")
                         .withGroup(Condition.Operator.AND, false)
                         .withGroup(Condition.Operator.OR, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("(false || false) should be false.")
                         .withGroup(Condition.Operator.AND, false)
                         .withGroup(Condition.Operator.OR, false)
                         .shouldBeFalse()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( true && true ) ) ==> true.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.OR, true)
                         .withGroup(Condition.Operator.AND, true)
                         .shouldBeTrue()
            );
            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( true && false ) ) ==> true.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.OR, true)
                         .withGroup(Condition.Operator.AND, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( false && true ) ) ==> true.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.OR, false)
                         .withGroup(Condition.Operator.AND, true)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (true || ( false && false ) ) ==> true.")
                         .withGroup(Condition.Operator.AND, true)
                         .withGroup(Condition.Operator.OR, false)
                         .withGroup(Condition.Operator.AND, false)
                         .shouldBeTrue()
            );

            data.add(new TestCase("A || B & C should evaluate as ( A || ( B && C) ):  (false || ( true && true ) ) ==> true.")
                         .withGroup(Condition.Operator.AND, false)
                         .withGroup(Condition.Operator.OR, true)
                         .withGroup(Condition.Operator.AND, true)
                         .shouldBeTrue()
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
        Rule rule = new Rule();
        return rule.evaluateConditions(aCase.request, aCase.response, aCase.groups);
    }

    private class TestCase {

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

        TestCase withGroup(Condition.Operator op, boolean result) {
            ConditionGroup g = mock(ConditionGroup.class);
            when(g.getOperator()).thenReturn(op);
            when(g.evaluate(Mockito.eq(request), Mockito.eq(response), Mockito.anyListOf(Condition.class))).thenReturn(result);
            groups.add(g);
            return this;
        }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}