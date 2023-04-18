package com.dotcms.experiments.business;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.analytics.model.AbstractAnalyticsProperties;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.exception.NotAllowedException;
import com.dotcms.experiments.business.result.BrowserSession;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.VariantResults;
import com.dotcms.experiments.business.result.VariantResults.ResultResumeItem;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goals;
import com.dotcms.http.server.mock.MockHttpServer;
import com.dotcms.http.server.mock.MockHttpServerContext;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.network.IPUtils;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.bytebuddy.utility.RandomString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link ExperimentsAPIImpl}
 */
public class ExperimentAPIImpIT extends IntegrationTestBase {
    
    private static final String CUBEJS_SERVER_IP = "127.0.0.1";
    private static final int CUBEJS_SERVER_PORT = 5000;
    private static final String CUBEJS_SERVER_URL = String.format("http://%s:%s", CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
    private static final DateTimeFormatter SIMPLE_FORMATTER = DateTimeFormatter
        .ofPattern("MM/dd/yyyy")
        .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter EVENTS_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
        .withZone(ZoneId.systemDefault());

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentsAPI#getRunningExperiments()}
     * When: You have tree Experiment:
     * - First one in DRAFT state, it has never been running.
     * - Second one in RUNNING state, it was already started and it was not stopped yet
     * - Finally one in STOP State, it was started, but it was stopped already
     */
    @Test
    public void getRunningExperiment() throws DotDataException, DotSecurityException {
        final Experiment draftExperiment = new ExperimentDataGen().nextPersisted();

        final Experiment runningExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(runningExperiment);

        final Experiment stoppedExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(stoppedExperiment);

        try {
            List<Experiment> experimentRunning = APILocator.getExperimentsAPI()
                    .getRunningExperiments();

            List<String> experimentIds = experimentRunning.stream()
                    .map(Experiment::getIdentifier).collect(Collectors.toList());

            assertTrue(experimentIds.contains(runningExperiment.getIdentifier()));
            assertFalse(experimentIds.contains(draftExperiment.getIdentifier()));
            assertTrue(experimentIds.contains(stoppedExperiment.getIdentifier()));
            assertCachedRunningExperiments(experimentIds);

            ExperimentDataGen.end(stoppedExperiment);

            experimentRunning = APILocator.getExperimentsAPI()
                    .getRunningExperiments();
            experimentIds = experimentRunning.stream()
                    .map(Experiment::getIdentifier).collect(Collectors.toList());

            assertTrue(experimentIds.contains(runningExperiment.getIdentifier()));
            assertFalse(experimentIds.contains(draftExperiment.getIdentifier()));
            assertFalse(experimentIds.contains(stoppedExperiment.getIdentifier()));
            assertCachedRunningExperiments(experimentIds);
        } finally {
            ExperimentDataGen.end(runningExperiment);

            final Optional<Experiment> experiment = APILocator.getExperimentsAPI()
                    .find(stoppedExperiment.getIdentifier(), APILocator.systemUser());

            if (experiment.isPresent() && experiment.get().status() == Status.RUNNING) {
                ExperimentDataGen.end(experiment.get());
            }
        }
    }

    private static void assertCachedRunningExperiments(final List<String> experimentIds) {
        CacheLocator.getExperimentsCache()
            .getList(ExperimentsCache.CACHED_EXPERIMENTS_KEY)
            .forEach(experiment -> assertTrue(experimentIds.contains(experiment.getIdentifier())));
    }

    /**
     * Method to test: {@link ExperimentsAPI#start(String, User)}
     * When: an {@link Experiment} is started
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test
    public void testStartExperiment_shouldPublishContent()
            throws DotDataException, DotSecurityException {

        final Experiment newExperiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        try {

            final String pageId = newExperiment.pageId();
            final Container container = new ContainerDataGen().nextPersisted();

            final List<Optional<Variant>> variants =
                    newExperiment.trafficProportion().variants().stream().map(
                                    variant ->
                                            Try.of(() -> APILocator.getVariantAPI().get(variant.id()))
                                                    .getOrNull())
                            .collect(Collectors.toList());

            final Variant variant1 = variants.get(0).orElseThrow();
            final Variant variant2 = variants.get(1).orElseThrow();

            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            final Contentlet content1Working = new ContentletDataGen(contentType).variant(variant1)
                    .nextPersisted();
            final Contentlet content2Working = new ContentletDataGen(contentType).variant(variant2)
                    .nextPersisted();
            final Contentlet content1Live = new ContentletDataGen(contentType).variant(variant1)
                    .nextPersisted();
            final Contentlet content2Live = new ContentletDataGen(contentType).variant(variant2)
                    .nextPersisted();

            final Contentlet pageAsContentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(pageId);
            final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(pageAsContentlet);

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content1Working).nextPersisted();

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content2Working).nextPersisted();

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content1Live).nextPersisted();

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content2Live).nextPersisted();

            ExperimentDataGen.start(newExperiment);
            assertCachedRunningExperiments(APILocator.getExperimentsAPI()
                .getRunningExperiments()
                .stream()
                .map(Experiment::getIdentifier)
                .collect(Collectors.toList()));

            List<Contentlet> experimentContentlets = APILocator.getContentletAPI()
                    .getAllContentByVariants(APILocator.systemUser(),
                            false, newExperiment.trafficProportion().variants().stream()
                                    .map(ExperimentVariant::id).filter((id) -> !id.equals(DEFAULT_VARIANT.name()))
                                    .toArray(String[]::new));

            experimentContentlets.forEach((contentlet -> {
                assertTrue(Try.of(contentlet::isLive).getOrElse(false));
            }));

        } catch(Exception e) {
            Logger.error(this, e);
            throw e;
        }finally {
            APILocator.getExperimentsAPI().end(newExperiment.id().orElseThrow()
                    , APILocator.systemUser());
            assertCachedRunningExperiments(APILocator.getExperimentsAPI()
                .getRunningExperiments()
                .stream()
                .map(Experiment::getIdentifier)
                .collect(Collectors.toList()));
        }

    }

    /*
     * Method to test: {@link ExperimentsAPI#start(String, User)}
     * When: an {@link Experiment} is started
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddMoreThanOneOriginalVariant_shouldFail() throws DotDataException, DotSecurityException {
        final Experiment newExperiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        APILocator.getExperimentsAPI().addVariant(newExperiment.id().orElse(""), ORIGINAL_VARIANT,
                APILocator.systemUser());
    }

    /**
     * Method to test: {@link ExperimentsAPI#deleteVariant(String, String, User)} (String, User)}
     * When: Try to delete the Experiment Original variant
     * Should: throw a NotAllowedException
     */
    @Test(expected = NotAllowedException.class)
    public void testADeleteOriginalVariant_shouldFail() throws DotDataException, DotSecurityException {
        final Experiment newExperiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        final ExperimentVariant originalVariant = newExperiment.trafficProportion()
                .variants().stream().filter((experimentVariant ->
                        experimentVariant.description().equals(ORIGINAL_VARIANT))).findFirst()
                .orElseThrow(()->new DotStateException("Unable to find Original Variant"));

        APILocator.getExperimentsAPI().deleteVariant(newExperiment.id().orElse(""), originalVariant.id(),
                APILocator.systemUser());
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getEvents(Experiment)}
     * When: You have 4 pages let call them: A, B, C and D and:
     * - We create a Experiment with the B page.
     * - We mock in the test 4 Browser Session with different lookBackWindows, each of this session
     * navigate by the follow pages.
     * Session 1: A, B, D and C
     * Session 2: D and C
     * Session 3: A and B
     * Session 4: A, B D and C
     *
     * All these navigation cases trigger a pageview Event for each page, so we should got these number
     * of events for each session:
     *
     * Session 1: 4
     * Session 2: 2
     * Session 3: 2
     * Session 4: 4
     *
     * If we call the {@link ExperimentsAPIImpl#getEvents(Experiment)} now
     *
     * Should: Return 4 {@link BrowserSession} each one with the right numbers of {@link com.dotcms.experiments.business.result.Event}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getEvents() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final String variantName_1 = "experiment_page_reach+testing_1_variant_1";
        final String variantName_2 = "experiment_page_reach+testing_1_variant_2";

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_events_testing_1",
                pageB, pageD);

        final List<Map<String, String>> events_1 = createPageViewEvents(Instant.now(), experiment,
                variantName_1, pageA, pageB, pageD, pageC);

        final List<Map<String, String>> events_2 = createPageViewEvents(Instant.now(), experiment,
                variantName_1, pageD, pageC);

        final List<Map<String, String>> events_3 = createPageViewEvents(Instant.now(), experiment,
                variantName_2, pageA, pageB);

        final List<Map<String, String>> events_4 = createPageViewEvents(Instant.now(), experiment,
                variantName_2, pageA, pageB, pageD, pageC);

        final Map<String, List<Map<String, String>>> sessions = new LinkedHashMap<>();
        sessions.put(events_1.get(0).get("Events.lookBackWindow"), events_1);
        sessions.put(events_2.get(0).get("Events.lookBackWindow"), events_2);
        sessions.put(events_3.get(0).get("Events.lookBackWindow"), events_3);
        sessions.put(events_4.get(0).get("Events.lookBackWindow"), events_4);


        final List<Map<String, String>> cubeJsQueryData = new ArrayList();
        cubeJsQueryData.addAll(events_1);
        cubeJsQueryData.addAll(events_2);
        cubeJsQueryData.addAll(events_3);
        cubeJsQueryData.addAll(events_4);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockHttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        IPUtils.disabledIpPrivateSubnet(true);

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final List<BrowserSession> browserSessions = experimentsAPIImpl.getEvents(experiment);

            mockHttpServer.validate();

            assertEquals(4, browserSessions.size());

            for (final BrowserSession browserSession : browserSessions) {
                final List<Map<String, String>> expected = sessions.get(
                        browserSession.getLookBackWindow());
                assertEquals(expected.size(), browserSession.getEvents().size());
                int i = 0;
                for (Map<String, String> expectedMap : expected) {
                    for (String key : expectedMap.keySet()) {
                        final String eventKey = key.replace("Events.", "");
                        assertEquals(expectedMap.get(key),
                                browserSession.getEvents().get(i).get(eventKey).get());
                    }

                    i++;
                }
            }

        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockHttpServer.stop();
        }
    }

    private static Experiment createExperimentWithReachPageGoalAndVariant(final String experimentName,
                                                                          final HTMLPageAsset experimentPage,
                                                                          final HTMLPageAsset reachPage,
                                                                          final String... vatiants) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(
                    getUrlCondition(reachPage.getPageUrl()),
                    getRefererCondition(experimentPage.getPageUrl()))
                .build();
        return createExperiment(experimentName, experimentPage, metric, vatiants);
    }

    private static Experiment createExperiment(final String experimentName,
                                               final HTMLPageAsset pageB,
                                               final Metric metric,
                                               final String... variants) {
        final Goals goal = Goals.builder().primary(metric).build();
        final ExperimentDataGen experimentDataGen = new ExperimentDataGen()
            .addVariant(experimentName)
            .page(pageB)
            .addGoal(goal);
        for (final String variant : variants) {
            experimentDataGen.addVariant(variant);
        }

        return experimentDataGen.nextPersisted();
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

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, B, D and C
     *
     * Should:  get a Success PAGE_REACH
     */
    @Test
    public void pageReachDirectReferer() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {
                final int sessionExpected = variantResult.getVariantName().equals(variantName) ? 1 : 0;

                Assert.assertEquals(sessionExpected, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(sessionExpected, variantResult.getMultiBySession());
                Assert.assertEquals(sessionExpected, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                final Map<String, ResultResumeItem> details = variantResult.getDetails();
                final ResultResumeItem resultResumeItem = details.get(
                        SIMPLE_FORMATTER.format(firstEventStartDate));

                assertNotNull(resultResumeItem);

                Assert.assertEquals(sessionExpected, resultResumeItem.getUniqueBySession());
                Assert.assertEquals(sessionExpected, resultResumeItem.getMultiBySession());
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, C, D and B
     *
     * Should:  get a Failed PAGE_REACH
     */
    @Test
    public void pageReachNotDirectReferer() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC, pageD, pageB );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {
                    Assert.assertEquals(1, (long) experimentResults.getSessions().getVariants().get(variantName));
                } else {
                    Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));
                }

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));
                assertNull(resultResumeItem);
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A and C
     *
     * Should:  get a Failed PAGE_REACH
     */
    @Test
    public void pageNotReachButExperimentPageWasView() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));
        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(0, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());
                Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));
                assertNull(resultResumeItem);
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, C and D
     *
     * Should:  get a Failed PAGE_REACH
     */
    @Test
    public void pageReachButExperimentPageWasNotView() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC, pageD );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(0, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());
                Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));
                assertNull(resultResumeItem);
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, B, C and D
     *
     * Should:  get a Faliled PAGE_REACH
     */
    @Test
    public void pageReachAfterExperimentPageButNotDirectReferer() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageB, pageC, pageD);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    Assert.assertEquals(1, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));
                } else {
                    Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));
                }

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));
                assertNull(resultResumeItem);
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have 3 pages: A, B and C
     * - You create an {@link Experiment} using the B page with a BOUNCE_RATE Goal: url EQUALS TO PAge B .
     * - You have the follow page_view to the pages order by timestamp: A, C and B
     *
     * Should:  count 1 Bounce Rate
     */
    @Test
    public void bounceRate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant("experiment_page_reach_testing_1",
                pageB);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC, pageB);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResult = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getSessions().getTotal());

            for (VariantResults variantResult : experimentResult.getGoals().get("primary").getVariants().values()) {
                final int sessionExpected =
                        variantResult.getVariantName().equals(variantName) ? 1 : 0;

                Assert.assertEquals(sessionExpected, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(sessionExpected, variantResult.getMultiBySession());
                Assert.assertEquals(sessionExpected,
                        (long) experimentResult.getSessions().getVariants()
                                .get(variantResult.getVariantName()));

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));

                assertNotNull(resultResumeItem);

                Assert.assertEquals(sessionExpected, resultResumeItem.getUniqueBySession());
                Assert.assertEquals(sessionExpected, resultResumeItem.getMultiBySession());
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have 3 pages: A, B and C
     * - You create an {@link Experiment} using the B page with a BOUNCE_RATE Goal: url EQUALS TO PAge B .
     * - You have the follow page_view to the pages order by timestamp: A, B and  C
     *
     * Should:  not count any Bounce Rate
     */
    @Test
    public void notBounceRate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant("experiment_page_reach_testing_1",
                pageB);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageB, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    Assert.assertEquals(1, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));
                } else {
                    Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));
                }

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));

                assertNull(resultResumeItem);
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have 3 pages: A, B and C
     * - You create an {@link Experiment} using the B page with a BOUNCE_RATE Goal: url EQUALS TO PAge B .
     * - You have the follow page_view to the pages order by timestamp: A and C
     *
     * Should:  Not Sessions into Experiment
     */
    @Test
    public void notBounceRateSession() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant("experiment_page_reach_testing_1",
                pageB);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResult = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(0, experimentResult.getSessions().getTotal());

            for (VariantResults variantResult : experimentResult.getGoals().get("primary").getVariants().values()) {

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());

                Assert.assertEquals(0, (long) experimentResult.getSessions().getVariants().get(variantResult.getVariantName()));

                final ResultResumeItem resultResumeItem = variantResult.getDetails()
                        .get(SIMPLE_FORMATTER.format(firstEventStartDate));
                assertNull(resultResumeItem);
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, B, D and C
     * it happened twice in different days.
     *
     * Should:  get a Success PAGE_REACH
     */
    @Test
    public void pageReachDirectRefererMultiDays() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final Instant firstBrowserSessionDate = Instant.now();
        final List<Map<String, String>> firstCubeJsQueryData = createPageViewEvents(firstBrowserSessionDate,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final Instant secondBrowserSessionDate = Instant.now().plus(1, ChronoUnit.DAYS);
        final List<Map<String, String>> secondCubeJsQueryData = createPageViewEvents(secondBrowserSessionDate,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final List<Map<String, String>> cubeJsQueryData = Stream.concat(firstCubeJsQueryData.stream(),
                secondCubeJsQueryData.stream()).collect(Collectors.toList());

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);

            mockhttpServer.validate();

            assertEquals(2, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {
                final int totalSessionExpected = variantResult.getVariantName().equals(variantName) ? 2 : 0;

                Assert.assertEquals(totalSessionExpected, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(totalSessionExpected, variantResult.getMultiBySession());
                Assert.assertEquals(totalSessionExpected, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                if (variantResult.getVariantName().equals(variantName)) {
                    final ResultResumeItem resultResumeItem = variantResult.getDetails()
                            .get(SIMPLE_FORMATTER.format(firstBrowserSessionDate));
                    assertNotNull(resultResumeItem);
                    Assert.assertEquals(1, resultResumeItem.getUniqueBySession());
                    Assert.assertEquals(1, resultResumeItem.getMultiBySession());

                    final ResultResumeItem resultResumeItem_2 = variantResult.getDetails()
                            .get(SIMPLE_FORMATTER.format(secondBrowserSessionDate));
                    assertNotNull(resultResumeItem_2);
                    Assert.assertEquals(1, resultResumeItem_2.getUniqueBySession());
                    Assert.assertEquals(1, resultResumeItem_2.getMultiBySession());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private Experiment createExperimentWithBounceRateGoalAndVariant(
            final String experimentName, final HTMLPageAsset experimentPage) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .addConditions(getUrlCondition(experimentPage.getPageUrl()))
                .build();

        return createExperiment(experimentName, experimentPage, metric);
    }

    private List<Map<String, String>> createPageViewEvents(final Instant firstEventTime,
            final Experiment experiment,
            final String variantName,
            final HTMLPageAsset... pages) throws DotDataException {

        final String lookBackWindows = new RandomString(20).nextString();
        final List<Map<String, String>> dataList = new ArrayList<>();
        Instant nextEventTriggerTime = firstEventTime;
        HTMLPageAsset previousPage = null;

        for (final HTMLPageAsset page : pages) {
            dataList.add(map(
                    "Events.experiment", experiment.getIdentifier(),
                    "Events.variant", variantName,
                    "Events.utcTime", EVENTS_FORMATTER.format(nextEventTriggerTime),
                    "Events.referer", Optional
                        .ofNullable(previousPage)
                        .map(HTMLPageAsset::getPageUrl)
                        .orElse(StringPool.BLANK),
                    "Events.url", "http://127.0.0.1:5000" + page.getURI(),
                    "Events.lookBackWindow", lookBackWindows,
                    "Events.eventType", EventType.PAGE_VIEW.getName()
            ));

            nextEventTriggerTime = nextEventTriggerTime.plus(1, ChronoUnit.MILLIS);
            previousPage = page;
        }
        return dataList;
    }

    private List<Map<String, String>> createPageViewEvents(final Instant firstEventTime,
                                                           final Experiment experiment,
                                                           final String variantName,
                                                           final int times,
                                                           final HTMLPageAsset... pages) throws DotDataException {
        final int repeated = Math.max(times, 1);
        final List<HTMLPageAsset> repeatedPages = new ArrayList<>(pages.length * repeated);
        IntStream.range(0, repeated).forEach(i -> repeatedPages.addAll(Arrays.asList(pages)));
        return createPageViewEvents(
            firstEventTime,
            experiment,
            variantName,
            repeatedPages.toArray(new HTMLPageAsset[0]));
    }

    private List<Map<String, String>> createPageViewEvents(final int sessions,
                                                           final Experiment experiment,
                                                           final String variantName,
                                                           final int times,
                                                           final HTMLPageAsset... pages) {
        final List<Map<String, String>> pageViews = new ArrayList<>(sessions);
        IntStream.range(0,  Math.max(sessions, 1)).forEach(i -> {
            try {
                pageViews.addAll(createPageViewEvents(Instant.now(), experiment, variantName, times, pages));
            } catch (DotDataException e) {
                throw new RuntimeException(e);
            }
        });
        return pageViews;
    }

    private static String getExpectedPageReachQuery(Experiment experiment) {
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
        return cubeJSQueryExpected;
    }

    private static String getExpectedBounceRateQuery(Experiment experiment) {
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
        return cubeJSQueryExpected;
    }

    private static AnalyticsHelper mockAnalyticsHelper() throws DotDataException, DotSecurityException {
        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
        final AbstractAnalyticsProperties mockAnalyticsProperties = mock(AbstractAnalyticsProperties.class);
        when(mockAnalyticsProperties.analyticsReadUrl()).thenReturn(CUBEJS_SERVER_URL);

        final AnalyticsApp mockAnalyticsApp = mock(AnalyticsApp.class);
        when(mockAnalyticsApp.getAnalyticsProperties()).thenReturn(mockAnalyticsProperties);

        final AnalyticsHelper mockAnalyticsHelper = mock(AnalyticsHelper.class);
        when(mockAnalyticsHelper.appFromHost(currentHost)).thenReturn(mockAnalyticsApp);
        return mockAnalyticsHelper;
    }

    private static MockHttpServer createMockHttpServerAndStart(final String expectedQuery, final String responseBody) {
        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
        final MockHttpServerContext mockHttpServerContext = new  MockHttpServerContext.Builder()
                .uri("/cubejs-api/v1/load")
                .requestCondition("Cube JS Query is not right",
                        context -> context.getRequestParameter("query")
                                .orElse(StringPool.BLANK)
                                .equals(expectedQuery))
                .responseStatus(HttpURLConnection.HTTP_OK)
                .mustBeCalled()
                .responseBody(responseBody)
                .build();

        mockhttpServer.addContext(mockHttpServerContext);
        mockhttpServer.start();

        return mockhttpServer;
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Try to save a Experiment with a REACh PAGE goal and it does not have ane referer parameter set
     * Should: set this parameter automatically to be CONTAINS the Experiment's page URL
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void addRefererConditionToReachPageGoal() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(metric).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Experiment Variant")
                .page(experimentPage)
                .addVariant("description")
                .addGoal(goal)
                .nextPersisted();

        final Experiment experimentFromDataBase = APILocator.getExperimentsAPI()
                .find(experiment.id().orElseThrow(), APILocator.systemUser())
                .orElseThrow();

        final Goals goals = experimentFromDataBase.goals().orElseThrow();
        final ImmutableList<Condition> conditions = goals.primary().conditions();

        assertEquals(2, conditions.size());

        for (final Condition condition : conditions) {
            if (condition.parameter().equals("url")) {
                assertEquals(reachPage.getPageUrl(), condition.value());
                assertEquals(Operator.CONTAINS, condition.operator());
            } else if (condition.parameter().equals("referer")) {
                assertEquals(experimentPage.getURI(), condition.value());
                assertEquals(Operator.CONTAINS, condition.operator());
            }
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When: Try to get the result from a not starting {@link Experiment}
     * Should: Throw a {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void tryToGetResultFromExperimentNotStarted()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Experiment Variant")
                .page(experimentPage)
                .addVariant("description")
                .nextPersisted();
        APILocator.getExperimentsAPI().getResults(experiment);
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When: Try to get the result from a not saved {@link Experiment}
     * Should: Throw a {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void tryToGetResultFromExperimentNotSaved()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Experiment Variant")
                .page(experimentPage)
                .addVariant("description")
                .next();
        APILocator.getExperimentsAPI().getResults(experiment);
    }
    
    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Try to save a Experiment with a Bounce Rate goal and it does not have ane url parameter set
     * Should: set this parameter automatically to be CONTAINS the Experiment's page URL
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void addUrlConditionToBounceRateCondition() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .build();

        final Goals goal = Goals.builder().primary(metric).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Experiment Variant")
                .page(experimentPage)
                .addVariant("description")
                .addGoal(goal)
                .nextPersisted();

        final Experiment experimentFromDataBase = APILocator.getExperimentsAPI()
                .find(experiment.id().orElseThrow(), APILocator.systemUser())
                .orElseThrow();

        final Goals goals = experimentFromDataBase.goals().orElseThrow();
        final ImmutableList<Condition> conditions = goals.primary().conditions();

        assertEquals(1,  conditions.size());

        assertEquals("url", conditions.get(0).parameter());
        assertEquals(experimentPage.getURI(), conditions.get(0).value());
        assertEquals(Operator.CONTAINS, conditions.get(0).operator());
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#archive(String, User)}
     * When: an Ended Experiment is archived
     * Should: not call the validateSchedule method
     */
    @Test
    public void testSaveExperiment_shouldNotValidateSchedule() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .build();

        final Goals goal = Goals.builder().primary(metric).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Experiment Variant")
                .page(experimentPage)
                .addVariant("description")
                .addGoal(goal)
                .nextPersisted();


        final ExperimentsAPI experimentsAPI = APILocator.getExperimentsAPI();
        final ExperimentsAPI spiedExperimentAPI = spy(experimentsAPI);

        final Experiment started = spiedExperimentAPI.start(experiment.id().orElseThrow(), APILocator.systemUser());
        spiedExperimentAPI.end(started.id().orElseThrow(), APILocator.systemUser());
        spiedExperimentAPI.archive(started.id().orElseThrow(), APILocator.systemUser());

        verify(spiedExperimentAPI, never()).validateScheduling(started.scheduling().orElseThrow());

     }

     /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have three pages: A, B and C
     * - You create an {@link Experiment} using the A page with a PAGE_REACH Goal: url EQUALS TO Page C .
     * - You have the follow page_view to the pages order by timestamp: A, B and C
     *
     * Should: calculate the probability that B beats A is 0.99
     */
    @Test
    public void test_calcBayesian_BOverA() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(
            "experiment_page_reach_testing_1",
            pageA, pageC);
        final String variantName = experiment.trafficProportion().variants().stream()
            .map(ExperimentVariant::id)
            .filter(id -> !id.equals("DEFAULT"))
            .limit(1)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));
        final List<Map<String, String>> data = createPageViewEvents(50, experiment, variantName, 2, pageA, pageC);
        data.addAll(createPageViewEvents(10, experiment, variantName, 2, pageA, pageB));
        data.addAll(createPageViewEvents(16, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageC));
        data.addAll(createPageViewEvents(44, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageB));
        final Map<String, List<Map<String, String>>> cubeJsQueryResult = map("data", data);

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);
            assertEquals(120, experimentResults.getSessions().getTotal());

            final BayesianResult bayesianResult = experimentResults.getBayesianResult();
            Assert.assertTrue(bayesianResult.suggestedWinner().startsWith("dotexperiment-"));

            mockhttpServer.validate();
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B and C
     * - You create an {@link Experiment} using the A page with a PAGE_REACH Goal: url EQUALS TO Page C .
     * - You have the follow page_view to the pages order by timestamp: A, B and C
     *
     * Should: calculate the probability that B beats A by 0.02
     */
    @Test
    public void test_calcBayesian_AOverB() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(
            "experiment_page_reach_testing_1",
            pageA, pageC);
        final String variantName = experiment.trafficProportion().variants().stream()
            .map(ExperimentVariant::id)
            .filter(id -> !id.equals("DEFAULT"))
            .limit(1)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));
        final List<Map<String, String>> data = createPageViewEvents(35, experiment, variantName, 2, pageA, pageC);
        data.addAll(createPageViewEvents(25, experiment, variantName, 2, pageA, pageB));
        data.addAll(createPageViewEvents(45, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageC));
        data.addAll(createPageViewEvents(15, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageB));
        final Map<String, List<Map<String, String>>> cubeJsQueryResult = map("data", data);

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);
            assertEquals(120, experimentResults.getSessions().getTotal());

            final BayesianResult bayesianResult = experimentResults.getBayesianResult();
            Assert.assertEquals(0.02, bayesianResult.value(), 0.01);
            Assert.assertEquals(DEFAULT_VARIANT.name(), bayesianResult.suggestedWinner());

            mockhttpServer.validate();
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B and C
     * - You create an {@link Experiment} using the A page with a PAGE_REACH Goal: url EQUALS TO Page C .
     * - You have the follow page_view to the pages order by timestamp: A, B and C
     * Then:
     * - The experiment is ended
     * Should: calculate the probability that B beats A by 0.02
     */
    @Test
    public void test_calcBayesian_AOverB_ended() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(
            "experiment_page_reach_testing_1",
            pageA, pageC);
        final String variantName = experiment.trafficProportion().variants().stream()
            .map(ExperimentVariant::id)
            .filter(id -> !id.equals("DEFAULT"))
            .limit(1)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));
        final List<Map<String, String>> data = createPageViewEvents(35, experiment, variantName, 2, pageA, pageC);
        data.addAll(createPageViewEvents(25, experiment, variantName, 2, pageA, pageB));
        data.addAll(createPageViewEvents(45, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageC));
        data.addAll(createPageViewEvents(15, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageB));
        final Map<String, List<Map<String, String>>> cubeJsQueryResult = map("data", data);

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);
            assertEquals(120, experimentResults.getSessions().getTotal());

            final BayesianResult bayesianResult = experimentResults.getBayesianResult();
            Assert.assertEquals(0.02, bayesianResult.value(), 0.01);
            Assert.assertEquals(DEFAULT_VARIANT.name(), bayesianResult.suggestedWinner());

            mockhttpServer.validate();
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment)}
     * When:
     * - You have four pages: A, B and C
     * - You create an {@link Experiment} using the A page with a PAGE_REACH Goal: url EQUALS TO Page C .
     * - You have the follow page_view to the pages order by timestamp: A, B and C
     *
     * Should: calculate the probability that B beats A is 0.99
     */
    @Test
    public void test_calcBayesian_ABC() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final String experimentName = "experiment_page_reach_testing_1";
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(
            experimentName,
            pageA, pageC,
            "variantC");
        experiment.trafficProportion().variants().stream()
            .map(ExperimentVariant::id)
            .filter(id -> !id.equals("DEFAULT"))
            .limit(1)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));
        final List<ExperimentVariant> variants = new ArrayList<>(experiment.trafficProportion().variants());
        final String variantBName = variants.get(1).id();
        final String variantCName = variants.get(2).id();

        final List<Map<String, String>> data = createPageViewEvents(50, experiment, variantBName, 2, pageA, pageC);
        data.addAll(createPageViewEvents(10, experiment, variantBName, 2, pageA, pageB));
        data.addAll(createPageViewEvents(16, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageC));
        data.addAll(createPageViewEvents(44, experiment, DEFAULT_VARIANT.name(), 2, pageA, pageB));
        data.addAll(createPageViewEvents(55, experiment, variantCName, 2, pageA, pageC));
        data.addAll(createPageViewEvents(5, experiment, variantCName, 2, pageA, pageB));
        final Map<String, List<Map<String, String>>> cubeJsQueryResult = map("data", data);

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(experiment);
            assertEquals(180, experimentResults.getSessions().getTotal());

            final BayesianResult bayesianResult = experimentResults.getBayesianResult();
            Assert.assertEquals(variantCName, bayesianResult.suggestedWinner());

            mockhttpServer.validate();
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

}
