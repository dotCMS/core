package com.dotcms.experiments.business;

import com.dotcms.analytics.metrics.*;
import com.dotcms.datagen.*;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.util.StringPool;
import jdk.jshell.spi.ExecutionControl;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.*;

/**
 * test of {@link ExperimentUrlPatternCalculator}
 */
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

    /**
     * Method to test: {@link ExperimentUrlPatternCalculator#calculatePageUrlRegexPattern(Experiment)}
     * When: Exists a Published Vanity Url with the forwardTo equals to the URI og the Experiment's Page
     * Should: The regex returned by the method should match the VanityUrl's URI
     *
     * @throws DotDataException
     */
    @Test
    public void experimentWithVanityUrl() throws DotDataException {

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

        final Contentlet vanityUrl = new VanityUrlDataGen()
                .uri("/testing")
                .forwardTo(experimentPage.getURI())
                .action(200)
                .host(host)
                .languageId(experimentPage.getLanguageId())
                .nextPersistedAndPublish();

        final String regex = ExperimentUrlPatternCalculator.INSTANCE.calculatePageUrlRegexPattern(experiment);

        assertTrue(("http://localhost:8080/" + experimentPage.getPageUrl()).matches(regex));
        assertTrue(("http://localhost:8080/testing").matches(regex));

    }

    /**
     * Method to test: {@link ExperimentUrlPatternCalculator#calculatePageUrlRegexPattern(Experiment)}
     * When: Exists a Published Vanity Url with the forwardTo equals to the URI og the Experiment's Page but with not 200 action
     * Should: The regex returned by the method should NOT match the VanityUrl's URI
     *
     * @throws DotDataException
     */
    @Test
    public void experimentWithVanityUrlWithNot200Action() throws DotDataException {
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

        final Contentlet vanityUrl = new VanityUrlDataGen()
                .uri("/testing")
                .forwardTo(experimentPage.getURI())
                .action(301)
                .host(host)
                .languageId(experimentPage.getLanguageId())
                .nextPersistedAndPublish();

        final String regex = ExperimentUrlPatternCalculator.INSTANCE.calculatePageUrlRegexPattern(experiment);

        assertTrue(("http://localhost:8080/" + experimentPage.getPageUrl()).matches(regex));
        assertFalse(("http://localhost:8080/testing").matches(regex));
    }

    /**
     * Method to test: {@link ExperimentUrlPatternCalculator#calculatePageUrlRegexPattern(Experiment)}
     * When: Exists a UnPublished Vanity Url with the forwardTo equals to the URI og the Experiment's Page
     * Should: The regex returned by the method should NOT match the VanityUrl's URI
     *
     * @throws DotDataException
     */
    @Test
    public void experimentWithUnPublishedVanity() throws DotDataException {
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

        final Contentlet vanityUrl = new VanityUrlDataGen()
                .uri("/testing")
                .forwardTo(experimentPage.getURI())
                .action(301)
                .host(host)
                .languageId(experimentPage.getLanguageId())
                .nextPersisted();

        final String regex = ExperimentUrlPatternCalculator.INSTANCE.calculatePageUrlRegexPattern(experiment);

        assertTrue(("http://localhost:8080/" + experimentPage.getPageUrl()).matches(regex));
        assertFalse(("http://localhost:8080/testing").matches(regex));
    }

    /**
     * Method to test: {@link ExperimentUrlPatternCalculator#calculatePageUrlRegexPattern(Experiment)}
     * When: Exists 2 Published Vanity Url with the forwardTo equals to the URI og the Experiment's Page and action equals to 200
     * Should: The regex returned by the method should  match both the VanityUrl's URI
     *
     * @throws DotDataException
     */
    @Test
    public void experimentWithTwoPublishedVanity() throws DotDataException {
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

        final Contentlet vanityUrl_1 = new VanityUrlDataGen()
                .uri("/testing")
                .forwardTo(experimentPage.getURI())
                .action(200)
                .host(host)
                .languageId(experimentPage.getLanguageId())
                .nextPersistedAndPublish();

        final Contentlet vanityUrl_2 = new VanityUrlDataGen()
                .uri("/another_testing")
                .forwardTo(experimentPage.getURI())
                .action(200)
                .host(host)
                .languageId(experimentPage.getLanguageId())
                .nextPersistedAndPublish();

        final String regex = ExperimentUrlPatternCalculator.INSTANCE.calculatePageUrlRegexPattern(experiment);

        assertTrue(("http://localhost:8080/" + experimentPage.getPageUrl()).matches(regex));
        assertTrue(("http://localhost:8080/testing").matches(regex));
        assertTrue(("http://localhost:8080/another_testing").matches(regex));
    }
}
