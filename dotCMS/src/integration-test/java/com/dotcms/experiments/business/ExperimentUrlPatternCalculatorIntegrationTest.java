package com.dotcms.experiments.business;

import com.dotcms.analytics.metrics.*;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.util.StringPool;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.*;

public class ExperimentUrlPatternCalculatorIntegrationTest {


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method: {@link ExperimentUrlPatternCalculator#calculateTargetPageUrlPattern(HTMLPageAsset, Metric)}
     * When: The Experiment has a REACH_PAGE Goal and a Use the Equals Operator
     */
    @Test
    public void reachPageGoalAndEqualsOperator(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Condition<Object> condition = Condition.builder()
                .parameter("url")
                .value("http://localhost:8080/testing")
                .operator(AbstractCondition.Operator.EQUALS)
                .build();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(condition).build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final String targetRegularExpression =
                ExperimentUrlPatternCalculator.INSTANCE.calculateTargetPageUrlPattern(experimentPage, metric)
                        .orElseThrow();

        assertEquals("http://localhost:8080/testing", targetRegularExpression);
        assertTrue("http://localhost:8080/testing".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing2".matches(targetRegularExpression));
        assertFalse("http://localhost/testing2".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/".matches(targetRegularExpression));
        assertFalse("https://localhost:8080/testing".matches(targetRegularExpression));
    }

    /**
     * Method: {@link ExperimentUrlPatternCalculator#calculateTargetPageUrlPattern(HTMLPageAsset, Metric)}
     * When: The Experiment has a REACH_PAGE Goal and a Use the Equals Operator
     */
    @Test
    public void reachPageGoalAndContainsOperator(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Condition<Object> condition = Condition.builder()
                .parameter("url")
                .value("testing")
                .operator(AbstractCondition.Operator.CONTAINS)
                .build();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(condition).build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final String targetRegularExpression =
                ExperimentUrlPatternCalculator.INSTANCE.calculateTargetPageUrlPattern(experimentPage, metric)
                        .orElseThrow();

        assertEquals(".*testing.*", targetRegularExpression);
        assertTrue("http://localhost:8080/testing".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/aaa/testing".matches(targetRegularExpression));
        assertTrue("http://testing/aaa".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/aaa/testing/bbb".matches(targetRegularExpression));
        assertFalse("https://localhost:8080/aaaa".matches(targetRegularExpression));
        assertFalse("https://localhost:8080/bbb".matches(targetRegularExpression));
    }

    /**
     * Method: {@link ExperimentUrlPatternCalculator#calculateTargetPageUrlPattern(HTMLPageAsset, Metric)}
     * When: The Experiment has a URL_PARAMETER Goal and a Use the Equals Operator
     */
    @Test
    public void urlParameterGoalAndEqualsOperator(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Condition<Object> condition = Condition.builder()
                .parameter("queryParameter")
                .value(new QueryParameter("testParameter", "testValue"))
                .operator(AbstractCondition.Operator.EQUALS)
                .build();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.URL_PARAMETER)
                .addConditions(condition).build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final String targetRegularExpression =
                ExperimentUrlPatternCalculator.INSTANCE.calculateTargetPageUrlPattern(experimentPage, metric)
                        .orElseThrow();

        assertEquals(".*\\?(.*&)?testParameter=testValue(&.*)*", targetRegularExpression);
        assertTrue("http://localhost:8080/testing?testParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=testValue&anotherParameter2=anotherValue2".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing".matches(targetRegularExpression));
        assertFalse("ttp://localhost:8080/testing?anotherParameter=anotherValue".matches(targetRegularExpression));
        assertFalse("http://localhost/testParameter/testValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?testParameter=testValueDifferent".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?testParameter=DifferenttestValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?testParameterDifferent=testValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?DifferentTestParameter=testValue".matches(targetRegularExpression));
    }

    /**
     * Method: {@link ExperimentUrlPatternCalculator#calculateTargetPageUrlPattern(HTMLPageAsset, Metric)}
     * When: The Experiment has a REACH_PAGE Goal and a Use the Equals Operator
     */
    @Test
    public void urlParameterGoalAndContainsOperator(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Condition<Object> condition = Condition.builder()
                .parameter("queryParameter")
                .value(new QueryParameter("testParameter", "testValue"))
                .operator(AbstractCondition.Operator.CONTAINS)
                .build();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.URL_PARAMETER)
                .addConditions(condition).build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final String targetRegularExpression =
                ExperimentUrlPatternCalculator.INSTANCE.calculateTargetPageUrlPattern(experimentPage, metric)
                        .orElseThrow();

        assertEquals(".*\\?(.*&)?testParameter=.*testValue.*(&.*)*", targetRegularExpression);
        assertTrue("http://localhost:8080/testing?testParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=thisIsAnothertestValueIsNotthesame".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=thisIsAnothertestValueIsNotthesame".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=testValue&anotherParameter2=anotherValue2".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=thisIsAnothertestValueIsNotthesame&anotherParameter2=anotherValue2".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing".matches(targetRegularExpression));
        assertFalse("ttp://localhost:8080/testing?anotherParameter=anotherValue".matches(targetRegularExpression));
        assertFalse("http://localhost/testParameter/testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=testValueDifferent".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=DifferenttestValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?testParameterDifferent=testValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?DifferentTestParameter=testValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?testParameter=anyValue".matches(targetRegularExpression));
    }

    /**
     * Method: {@link ExperimentUrlPatternCalculator#calculateTargetPageUrlPattern(HTMLPageAsset, Metric)}
     * When: The Experiment has a REACH_PAGE Goal and a Use the Equals Operator
     */
    @Test
    public void urlParameterGoalAndExistsOperator(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Condition<Object> condition = Condition.builder()
                .parameter("queryParameter")
                .value(new QueryParameter("testParameter", StringPool.BLANK))
                .operator(AbstractCondition.Operator.EXISTS)
                .build();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.URL_PARAMETER)
                .addConditions(condition).build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final String targetRegularExpression =
                ExperimentUrlPatternCalculator.INSTANCE.calculateTargetPageUrlPattern(experimentPage, metric)
                        .orElseThrow();

        assertEquals(".*\\?(.*&)?testParameter=.*(&.*)*", targetRegularExpression);
        assertTrue("http://localhost:8080/testing?testParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=thisIsAnothertestValueIsNotthesame".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=thisIsAnothertestValueIsNotthesame".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=testValue&anotherParameter2=anotherValue2".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?anotherParameter=anotherValue&testParameter=thisIsAnothertestValueIsNotthesame&anotherParameter2=anotherValue2".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing".matches(targetRegularExpression));
        assertFalse("ttp://localhost:8080/testing?anotherParameter=anotherValue".matches(targetRegularExpression));
        assertFalse("http://localhost/testParameter/testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=testValueDifferent".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=DifferenttestValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?testParameterDifferent=testValue".matches(targetRegularExpression));
        assertFalse("http://localhost:8080/testing?DifferentTestParameter=testValue".matches(targetRegularExpression));
        assertTrue("http://localhost:8080/testing?testParameter=anyValue".matches(targetRegularExpression));
    }
}
