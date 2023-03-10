package com.dotcms.experiments.business;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.business.result.BrowserSession;
import com.dotcms.experiments.business.result.Event;
import com.dotcms.experiments.business.result.ExperimentAnalyzerUtil;

import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.VariantResults;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link ExperimentAnalyzerUtil}
 */
public class ExperimentAnalyzerUtilIT {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentAnalyzerUtil#getExperimentResult(Experiment, List)}
     * When:
     * - You have 4 pages: A, B, C and D
     * - Create a Experiment with a PAGE_REACH goal where:
     *      - Page B is the Experiment's Page
     *      - Page D is the PAGE to be reach
     * -  Now different users navigate by the site (triggering pageview Events)
     *      - A, B, D and C: Success.
     *      - A, B, C and D: Fail.
     *      - A, D and C: Not count as session into the Experiment
     *      - A: Not count as session into the Experiment
     *      - A, B, D, C, B and D: Success two times
     * Should:
     * - Total Session: 3
     * - Unique Session success: 2
     * - Multi Session: 3
     */
    @Test
    public void analyzerDataReachPage(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(pageD.getPageUrl()),
                        getRefererCondition(pageB.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(metric).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Testing Variant")
                .page(pageB)
                .addGoal(goal)
                .nextPersisted();

        final ExperimentVariant anotherVariant = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must a Variant diffrent from DEFAULT"));

        final List<Map<String, Object>> browserSession_1 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageB, pageD, pageC);

        final List<Map<String, Object>> browserSession_2 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageB, pageC, pageD);

        final List<Map<String, Object>> browserSession_3 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageD, pageC);

        final List<Map<String, Object>> browserSession_4 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA);

        final List<Map<String, Object>> browserSession_5 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageB, pageD, pageC, pageB, pageD);

        final List<BrowserSession> browserSessions = new ArrayList<>();
        browserSessions.add(new BrowserSession(
                browserSession_1.get(0).get("Events.lookBackWindow").toString(),
                browserSession_1.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        browserSessions.add(new BrowserSession(
                browserSession_2.get(0).get("Events.lookBackWindow").toString(),
                browserSession_2.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        browserSessions.add(new BrowserSession(
                browserSession_3.get(0).get("Events.lookBackWindow").toString(),
                browserSession_3.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        browserSessions.add(new BrowserSession(
                browserSession_4.get(0).get("Events.lookBackWindow").toString(),
                browserSession_4.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        browserSessions.add(new BrowserSession(
                browserSession_5.get(0).get("Events.lookBackWindow").toString(),
                browserSession_5.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        final ExperimentResults experimentResults = ExperimentAnalyzerUtil.INSTANCE
                .getExperimentResult(experiment, browserSessions);

        assertEquals(3, experimentResults.getSessions());

       assertEquals(1, experimentResults.getGoals().size());

        final Map<String, VariantResults> variants = experimentResults.getGoals().get(0)
                .getVariants();

        assertEquals(2, variants.size());

        final List<String> expectedVariants = experiment.trafficProportion().variants().stream()
                .map(experimentVariant -> experimentVariant.id())
                .collect(Collectors.toList());

        for (VariantResults resultVariant : variants.values()) {

            assertTrue(expectedVariants.contains(resultVariant.getVariantName()));

            if (!resultVariant.getVariantName().equals("DEFAULT")) {
                assertEquals(2, resultVariant.getUniqueBySession().getCount());
                assertEquals(3, resultVariant.getMultiBySession());
            } else {
                assertEquals(0, resultVariant.getUniqueBySession().getCount());
                assertEquals(0, resultVariant.getMultiBySession());
            }
        }

    }

    /**
     * Method to test: {@link ExperimentAnalyzerUtil#getExperimentResult(Experiment, List)}
     * When:
     * - You have 4 pages: A, B, and C
     * - Create a Experiment with a BOUNCE_RATE goal where:
     *      - Page B is the Experiment's Page and also if the page to be check for BOUNCE_RATE
     * -  Now different users navigate by the site (triggering pageview Events)
     *      - A, B: Count as a Bounce Rate.
     *      - A, C and D: Not count as session into the Experiment.
     *      - A, B, C: not count as a Bounce Rate
     * Should:
     * - Total Session: 2
     * - Unique Session Bounce Rate: 1
     * - Multi Session: 1 (Really we can not have more than one Bounce Rate by session)
     */
    @Test
    public void analyzerDataBounceRate(){
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .addConditions(getUrlCondition(pageB.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(metric).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Testing Variant")
                .page(pageB)
                .addGoal(goal)
                .nextPersisted();

        final ExperimentVariant anotherVariant = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must a Variant diffrent from DEFAULT"));

        final List<Map<String, Object>> browserSession_1 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageB);

        final List<Map<String, Object>> browserSession_2 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageC);

        final List<Map<String, Object>> browserSession_3 = createPageViewEvents(experiment,
                anotherVariant.id(), pageA, pageB, pageC);


        final List<BrowserSession> browserSessions = new ArrayList<>();
        browserSessions.add(new BrowserSession(
                browserSession_1.get(0).get("Events.lookBackWindow").toString(),
                browserSession_1.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        browserSessions.add(new BrowserSession(
                browserSession_2.get(0).get("Events.lookBackWindow").toString(),
                browserSession_2.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        browserSessions.add(new BrowserSession(
                browserSession_3.get(0).get("Events.lookBackWindow").toString(),
                browserSession_3.stream().map(eventMap -> new Event(eventMap, EventType.PAGE_VIEW)).collect(Collectors.toList())));

        final ExperimentResults experimentResult = ExperimentAnalyzerUtil.INSTANCE
                .getExperimentResult(experiment, browserSessions);

        assertEquals(2, experimentResult.getSessions());

        assertEquals(1, experimentResult.getGoals().size());

        final Map<String, VariantResults> variants = experimentResult.getGoals().get(0)
                .getVariants();

        assertEquals(2, variants.size());

        final List<String> expectedVariants = experiment.trafficProportion().variants().stream()
                .map(experimentVariant -> experimentVariant.id())
                .collect(Collectors.toList());

        for (VariantResults resultVariant : variants.values()) {

            assertTrue(expectedVariants.contains(resultVariant.getVariantName()));

            if (!resultVariant.getVariantName().equals("DEFAULT")) {
                assertEquals(1, resultVariant.getUniqueBySession().getCount());
                assertEquals(1, resultVariant.getMultiBySession());
            } else {
                assertEquals(0, resultVariant.getUniqueBySession().getCount());
                assertEquals(0, resultVariant.getMultiBySession());
            }
        }

    }

    private List<Map<String, Object>> createPageViewEvents(final Experiment experiment,
            final String variantName,
            final HTMLPageAsset... pages) {

        final String lookBackWindows = new RandomString(20).nextString();
        final List<Map<String, Object>> dataList = new ArrayList<>();
        Instant nextEventTriggerTime = Instant.now();
        HTMLPageAsset previousPage = null;

        for (final HTMLPageAsset page : pages) {
            dataList.add(map(
                    "Events.experiment", experiment.getIdentifier(),
                    "Events.variant", variantName,
                    "Events.utcTime", nextEventTriggerTime.toString(),//"2022-09-20T15:24:21.000",
                    "Events.referer", UtilMethods.isSet(previousPage) ?
                            "http://127.0.0.1:5000/" + previousPage.getPageUrl() : StringPool.BLANK,
                    "Events.url", "http://127.0.0.1:5000/" + page.getPageUrl(),
                    "Events.lookBackWindow", lookBackWindows
            ));

            nextEventTriggerTime = nextEventTriggerTime.plus(1, ChronoUnit.MILLIS);
            previousPage = page;
        }
        return dataList;
    }

    private static Condition getUrlCondition(final String url) {
        return Condition.builder()
                .parameter("url")
                .value(url)
                .operator(Operator.CONTAINS)
                .build();
    }

    private static Condition getRefererCondition(final String referer) {
        return Condition.builder()
                .parameter("referer")
                .value(referer)
                .operator(Operator.CONTAINS)
                .build();
    }

}
