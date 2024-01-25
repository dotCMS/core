package com.dotcms.experiments.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.business.result.ExperimentResultsQueryFactory;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link ExperimentResultsQueryFactory}
 */
public class ExperimentResultsQueryFactoryIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentResultsQueryFactory#create(Experiment)}
     * When: Try to get the {@link CubeJSQuery} to a Experiment with a REACH_PAGE GOal
     * Should: get the follow query:
     *
     * <code>
     * {
     *      "filters":[
     *          {
     *              "values":["pageview"],
     *              "member":"Events.eventType",
     *              "operator":"equals"
     *          },
     *          {
     *              "values":["e1aa92a0-d266-4482-bdf2-e0c1bbcf013a"],
     *              "member":"Events.experiment",
     *              "operator":"equals"
     *          }
     *      ],
     *      "dimensions":[
     *          "Events.referer",
     *          "Events.experiment",
     *          "Events.variant",
     *          "Events.utcTime",
     *          "Events.url",
     *          "Events.lookBackWindow"
     *      ],
     *      "order":{
     *          "Events.utcTime":"asc",
     *          "Events.lookBackWindow":"asc"
     *       }
     * }
     *
     * </code>
     */
    @Test
    public void createQueryForPageReachGoal()  {

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()), getRefererCondition(experimentPage.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("page_reach+testing_1")
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final CubeJSQuery cubeJSQuery = ExperimentResultsQueryFactory.INSTANCE.create(experiment);
        final String cubeJSQueryExpected ="{"
                +   "\"filters\":["
                +       "{"
                +           "\"values\":["
                +               "\"pageview\""
                +           "],"
                +           "\"member\":\"Events.eventType\","
                +           "\"operator\":\"equals\""
                +       "},"
                +       "{"
                +           "\"values\":["
                +               "\"" + experiment.getIdentifier() + "\""
                +           "],"
                +           "\"member\":\"Events.experiment\","
                +           "\"operator\":\"equals\""
                +       "}"
                +   "],"
                +   "\"dimensions\":["
                +       "\"Events.referer\","
                +       "\"Events.experiment\","
                +       "\"Events.variant\","
                +       "\"Events.utcTime\","
                +       "\"Events.url\","
                +       "\"Events.lookBackWindow\","
                +       "\"Events.eventType\""
                +   "],"
                +   "\"order\":{"
                +       "\"Events.lookBackWindow\":\"asc\","
                +       "\"Events.utcTime\":\"asc\""
                +   "}"
                + "}";

        assertEquals(cubeJSQueryExpected, cubeJSQuery.toString());
    }

    private static Condition getUrlCondition(final String url) {
        return Condition.builder()
                .parameter("url")
                .value(url)
                .operator(Operator.EQUALS)
                .build();
    }

    private static Condition getRefererCondition(final String referer) {
        return Condition.builder()
                .parameter("referer")
                .value(referer)
                .operator(Operator.EQUALS)
                .build();
    }

    /**
     * Method to test: {@link ExperimentResultsQueryFactory#create(Experiment)}
     * When: Try to get the {@link CubeJSQuery} to a Experiment with a EXIT_RATE GOal
     * Should: get the follow query:
     *
     * <code>
     * {
     *      "filters":[
     *          {
     *              "values":["pageview"],
     *              "member":"Events.eventType",
     *              "operator":"equals"
     *          },
     *          {
     *              "values":["e1aa92a0-d266-4482-bdf2-e0c1bbcf013a"],
     *              "member":"Events.experiment",
     *              "operator":"equals"
     *          }
     *      ],
     *      "dimensions":[
     *          "Events.experiment",
     *          "Events.variant",
     *          "Events.utcTime",
     *          "Events.url",
     *          "Events.lookBackWindow"
     *      ],
     *      "order":{
     *          "Events.utcTime":"asc",
     *          "Events.lookBackWindow":"asc"
     *       }
     * }
     *
     * </code>
     */
    @Test
    public void createQueryForBounceRateGoal()  {

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset bounceRatePage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Bounce Rate Metric")
                .type(MetricType.EXIT_RATE)
                .addConditions(getUrlCondition(bounceRatePage.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("page_reach+testing_1")
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final CubeJSQuery cubeJSQuery = ExperimentResultsQueryFactory.INSTANCE.create(experiment);
        final String cubeJSQueryExpected ="{"
                +   "\"filters\":["
                +       "{"
                +           "\"values\":["
                +               "\"pageview\""
                +           "],"
                +           "\"member\":\"Events.eventType\","
                +           "\"operator\":\"equals\""
                +       "},"
                +       "{"
                +           "\"values\":["
                +               "\"" + experiment.getIdentifier() + "\""
                +           "],"
                +           "\"member\":\"Events.experiment\","
                +           "\"operator\":\"equals\""
                +       "}"
                +   "],"
                +   "\"dimensions\":["
                +       "\"Events.experiment\","
                +       "\"Events.variant\","
                +       "\"Events.utcTime\","
                +       "\"Events.url\","
                +       "\"Events.lookBackWindow\","
                +       "\"Events.eventType\""
                +   "],"
                +   "\"order\":{"
                +       "\"Events.lookBackWindow\":\"asc\","
                +       "\"Events.utcTime\":\"asc\""
                +   "}"
                + "}";

        assertEquals(cubeJSQueryExpected, cubeJSQuery.toString());
    }
}
