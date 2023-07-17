package com.dotcms.experiments.business;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static com.dotcms.util.CollectionsUtils.list;
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
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.analytics.model.AnalyticsProperties;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.exception.NotAllowedException;
import com.dotcms.experiments.business.result.BrowserSession;
import com.dotcms.experiments.business.result.ExperimentAnalyzerUtil;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.GoalResults;
import com.dotcms.experiments.business.result.VariantResults;
import com.dotcms.experiments.business.result.VariantResults.ResultResumeItem;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.RunningIds.RunningId;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.http.server.mock.MockHttpServer;
import com.dotcms.http.server.mock.MockHttpServerContext;
import com.dotcms.http.server.mock.MockHttpServerContext.RequestContext;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.network.IPUtils;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.control.Try;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.bytebuddy.utility.RandomString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of {@link ExperimentsAPIImpl}
 */
@RunWith(DataProviderRunner.class)
public class ExperimentAPIImpIntegrationTest extends IntegrationTestBase {
    
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
     * Method to test: {@link ExperimentsAPIImpl#start(long, User)}
     * When: The experiment is started
     * Should: Generate a new Running Experiment ID
     */
    @Test
    public void createRunningIdWhenExperimentIsTurnToRunning()
            throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        final Experiment experimentFromDataBase_1 = APILocator.getExperimentsAPI()
                .find(experiment.id().get(), APILocator.systemUser())
                .orElseThrow(() -> new AssertionError("Experiment not found"));

        assertNotNull(experimentFromDataBase_1.runningIds());

        assertFalse(experimentFromDataBase_1.runningIds().iterator().hasNext());

        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .start(experiment.id().get(), APILocator.systemUser());

        try {
            assertNotNull(experimentStarted.runningIds());
            assertEquals(1, experimentStarted.runningIds().size());
            final RunningId runningId = experimentStarted.runningIds().iterator().next();
            assertNotNull(runningId);
            assertNotNull(runningId.id());
            assertNotNull(runningId.startDate());
            assertNull(runningId.endDate());

            final Experiment experimentFromDataBase_2 = APILocator.getExperimentsAPI()
                    .find(experiment.id().get(), APILocator.systemUser())
                    .orElseThrow(() -> new AssertionError("Experiment not found"));

            assertNotNull(experimentFromDataBase_2.runningIds());

            final RunningId runningIdFromDataBase_2 = experimentFromDataBase_2.runningIds()
                    .iterator().next();
            assertNotNull(runningIdFromDataBase_2);
            assertNotNull(runningIdFromDataBase_2.id());
            assertNotNull(runningIdFromDataBase_2.startDate());
            assertNull(runningIdFromDataBase_2.endDate());
        } finally {
            APILocator.getExperimentsAPI().end(experimentStarted.id().get(), APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(long, User)}
     * When: The experiment is started but using {@link ExperimentsAPIImpl#startScheduled(long, User)}
     * Should: Generate a new Running Experiment ID
     */
    @Test
    public void createRunningIdWithStartScheduled()
            throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen()
                .status(Status.SCHEDULED)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(2, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        final Experiment experimentFromDataBase_1 = APILocator.getExperimentsAPI()
                .find(experiment.id().get(), APILocator.systemUser())
                .orElseThrow(() -> new AssertionError("Experiment not found"));

        assertNotNull(experimentFromDataBase_1.runningIds());

        assertFalse(experimentFromDataBase_1.runningIds().iterator().hasNext());

        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .startScheduled(experiment.id().get(), APILocator.systemUser());

        try {
            assertNotNull(experimentStarted.runningIds());
            assertEquals(1, experimentStarted.runningIds().size());
            final RunningId runningId = experimentStarted.runningIds().iterator().next();
            assertNotNull(runningId);
            assertNotNull(runningId.id());
            assertNotNull(runningId.startDate());
            assertNull(runningId.endDate());

            final Experiment experimentFromDataBase_2 = APILocator.getExperimentsAPI()
                    .find(experiment.id().get(), APILocator.systemUser())
                    .orElseThrow(() -> new AssertionError("Experiment not found"));

            assertNotNull(experimentFromDataBase_2.runningIds());

            final RunningId runningIdFromDataBase_2 = experimentFromDataBase_2.runningIds()
                    .iterator().next();
            assertNotNull(runningIdFromDataBase_2);
            assertNotNull(runningIdFromDataBase_2.id());
            assertNotNull(runningIdFromDataBase_2.startDate());
            assertNull(runningIdFromDataBase_2.endDate());
        } finally {
            APILocator.getExperimentsAPI().end(experimentStarted.id().get(), APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(long, User)}
     * When: The experiment is started twice
     * Should: Generate two Running Experiment ID different
     */
    @Test
    public void restartExperimentMustGenerateTwoRunningIds()
            throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .start(experiment.id().get(), APILocator.systemUser());

        try {
            final Experiment experimentToRestart = Experiment.builder().from(experimentStarted)
                    .status(Status.DRAFT)
                    .scheduling(Optional.empty())
                    .build();

           FactoryLocator.getExperimentsFactory().save(experimentToRestart);

            APILocator.getExperimentsAPI()
                    .start(experimentToRestart.id().get(), APILocator.systemUser());

            final Experiment experimentAfterReStart = APILocator.getExperimentsAPI()
                    .find(experimentToRestart.id().get(), APILocator.systemUser())
                    .orElseThrow(() -> new AssertionError("Experiment not found"));

            assertEquals(2, experimentAfterReStart.runningIds().size());

            assertTrue(experimentAfterReStart.runningIds().getAll().stream()
                    .anyMatch(runningId -> runningId.endDate() != null));

            assertTrue(experimentAfterReStart.runningIds().getAll().stream()
                    .anyMatch(runningId -> runningId.endDate() == null));

            assertTrue(experimentAfterReStart.runningIds().get(0).id() != experimentAfterReStart.runningIds().get(1).id());
        } finally {
            APILocator.getExperimentsAPI().end(experimentStarted.id().get(), APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#startScheduled(String, User)}
     * When: The experiment is started twice using the {@link ExperimentsAPIImpl#startScheduled(String, User)}
     * Should: Generate two Running Experiment ID different
     */
    @Test
    public void restartExperimentUsingTheStartScheduled()
            throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen()
                .status(Status.SCHEDULED)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(2, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .startScheduled(experiment.id().get(), APILocator.systemUser());

        final Experiment experimentToRestart = Experiment.builder().from(experimentStarted)
                .status(Status.SCHEDULED)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(2, ChronoUnit.DAYS))
                        .build())
                .build();

        try {
            FactoryLocator.getExperimentsFactory().save(experimentToRestart);

            APILocator.getExperimentsAPI()
                    .startScheduled(experimentToRestart.id().get(), APILocator.systemUser());

            final Experiment experimentAfterReStart = APILocator.getExperimentsAPI()
                    .find(experimentToRestart.id().get(), APILocator.systemUser())
                    .orElseThrow(() -> new AssertionError("Experiment not found"));

            assertEquals(2, experimentAfterReStart.runningIds().size());

            assertTrue(experimentAfterReStart.runningIds().getAll().stream()
                    .anyMatch(runningId -> runningId.endDate() != null));

            assertTrue(experimentAfterReStart.runningIds().getAll().stream()
                    .anyMatch(runningId -> runningId.endDate() == null));

            assertTrue(experimentAfterReStart.runningIds().get(0).id() != experimentAfterReStart.runningIds().get(1).id());
        } finally {
            APILocator.getExperimentsAPI().end(experimentToRestart.id().get(), APILocator.systemUser());
        }
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
     * Method to test: {@link ExperimentsAPI#getEvents(Experiment, User)}
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
     * If we call the {@link ExperimentsAPI#getEvents(Experiment, User)} now
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

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

        final MockHttpServer mockHttpServer = createMockHttpServer(
            cubeJSQueryExpected,
            JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        addCountQueryContext(experiment, cubeJsQueryData.size(), mockHttpServer);

        mockHttpServer.start();

        IPUtils.disabledIpPrivateSubnet(true);

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final List<BrowserSession> browserSessions = experimentsAPIImpl.getEvents(
                    experiment,
                    APILocator.systemUser());

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

    private static Experiment createExperimentWithReachPageGoalAndVariant(final HTMLPageAsset experimentPage,
            final HTMLPageAsset reachPage) {

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(
                        getUrlCondition(reachPage.getPageUrl()),
                        getRefererCondition(experimentPage.getPageUrl()))
                .build();
        return createExperiment(experimentPage, metric, new String[]{RandomString.make(15)});
    }

    private static Experiment createExperimentWithReachPageGoalAndVariant(final HTMLPageAsset experimentPage,
                                                                          final HTMLPageAsset reachPage,
                                                                            final String... variants) {

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(
                    getUrlCondition(reachPage.getPageUrl()),
                    getRefererCondition(experimentPage.getPageUrl()))
                .build();
        return createExperiment(experimentPage, metric, variants);
    }

    private static Experiment createExperiment(final HTMLPageAsset pageB,
                                               final Metric metric,
                                               final String... variants) {

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        final ExperimentDataGen experimentDataGen = new ExperimentDataGen()
            .page(pageB)
            .addGoal(goal);

        Arrays.stream(variants).forEach(variantName -> experimentDataGen.addVariant(variantName));

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When: get the Experiment's Results
     * Should: get the Split traffic too
     */
    @Test
    public void getSplitTrafficInsideExperimentResults()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageA, pageB);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        try {

            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

             addCountQueryContext(experiment, 0, mockhttpServer);

            ExperimentDataGen.start(experiment);

            final String noDefaultVariantName = experiment.trafficProportion().variants().stream()
                    .map(experimentVariant -> experimentVariant.id())
                    .filter(id -> !id.equals("DEFAULT"))
                    .findFirst()
                    .orElseThrow();

            final ExperimentsAPIImpl experimentsAPI = new ExperimentsAPIImpl(mockAnalyticsHelper);

            mockhttpServer.start();
            IPUtils.disabledIpPrivateSubnet(true);

            final ExperimentResults results = experimentsAPI.getResults(experiment, APILocator.systemUser());

            mockhttpServer.validate();

            final Map<String, VariantResults> variants = results.getGoals().get("primary")
                    .getVariants();
            assertEquals(2, variants.size());

            final VariantResults variantResults = variants.get(noDefaultVariantName);
            assertEquals(50f, variantResults.weight());

            final VariantResults controlResults = variants.get("DEFAULT");
            assertEquals(50f, controlResults.weight());
        } finally {
            ExperimentDataGen.end(experiment);
            mockhttpServer.stop();
        }
    }


    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);
        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "4")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 4, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(4, variantResult.getTotalPageViews());
                } else {

                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} with one Variant using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D.
     * - You have 1000 Sessions, 500 for each Variant (Default and Variant), in each session you have the folloe page_view:
     * A, B, D, C, A, B, D, C and A.
     * this is 9000 Events in total it means exactly 9 pages
     * - You are going to get as result the follow:
     * 1000 Total Sessions
     * 500 total Sessions foeach Variant
     * 500 Unique Session success for each Variant
     * 1000 Multi session success for each Variant (because page D is reach twice in each session)
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void resultWithPagination() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);
        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();

        final List<Map<String, String>> cubeJsQueryAllData = new ArrayList<>();

        for (int i= 0; i < 1000; i++) {
            final String variantToUse = i % 2 == 0 ? variantName : "DEFAULT";
            final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                    experiment, variantToUse, pageA, pageB, pageD, pageC, pageA, pageB, pageD, pageC, pageA);

            cubeJsQueryAllData.addAll(cubeJsQueryData);
        }

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        int offset = 0;
        for (int i = 0; i < 9; i++) {
            final List<Map<String, String>> page = new ArrayList<>(
                    cubeJsQueryAllData.subList(offset, offset + 1000));

            final Map<String, List<Map<String, String>>> pageCubeJsQueryResult =  map("data", page);

            final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment, 1000, offset);

            addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(pageCubeJsQueryResult));

            offset += 1000;
        }

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "4500"),
                map("Events.variant", "DEFAULT", "Events.count", "4500")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        final String countExpectedPageReachQuery = getCountExpectedPageReachQuery(experiment);
        final List<Map<String, Object>> countResponseExpected = list(
                map("Events.count", "9000")
        );

        addContext(mockhttpServer, countExpectedPageReachQuery,
                JsonUtil.getJsonStringFromObject(map("data", countResponseExpected)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1000, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                } else {

                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());
                }

                assertEquals(4500, variantResult.getTotalPageViews());

                Assert.assertEquals(500, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(1000, variantResult.getMultiBySession());
                Assert.assertEquals(500, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                final Map<String, ResultResumeItem> details = variantResult.getDetails();
                final ResultResumeItem resultResumeItem = details.get(
                        SIMPLE_FORMATTER.format(firstEventStartDate));

                assertNotNull(resultResumeItem);

                Assert.assertEquals(500, resultResumeItem.getUniqueBySession());
                Assert.assertEquals(1000, resultResumeItem.getMultiBySession());
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} with one Variant using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D.
     * - You have 500 Sessions, 250 for each Variant (Default and Variant), in each session you have the follow page_view:
     * A, B, D, C, A, B, D, C and A.
     * this is 4500 Events it means 5 pages but the last one jut it going to have 500 events
     * - You are going to get as result the follow:
     * 500 Total Sessions
     * 250 total Sessions for each Variant
     * 250 Unique Session success for each Variant
     * 500 Multi session success for each Variant (because page D is reach twice in each session)
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void resultWithPaginationWhenTheLastPageIsNotComplte() throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);
        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();

        final List<Map<String, String>> cubeJsQueryAllData = new ArrayList<>();

        for (int i= 0; i < 500; i++) {
            final String variantToUse = i % 2 == 0 ? variantName : "DEFAULT";
            final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                    experiment, variantToUse, pageA, pageB, pageD, pageC, pageA, pageB, pageD, pageC, pageA);

            cubeJsQueryAllData.addAll(cubeJsQueryData);
        }

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        int offset = 0;
        for (int i = 0; i < 5; i++) {
            final int totalItems = i ==4 ? 500 : 1000;
            final List<Map<String, String>> page = new ArrayList<>(
                    cubeJsQueryAllData.subList(offset, offset + totalItems));

            final Map<String, List<Map<String, String>>> pageCubeJsQueryResult =  map("data", page);

            final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment, 1000, offset);

            addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(pageCubeJsQueryResult));

            offset += 1000;
        }

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "2250"),
                map("Events.variant", "DEFAULT", "Events.count", "2250")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        final String countExpectedPageReachQuery = getCountExpectedPageReachQuery(experiment);
        final List<Map<String, Object>> countResponseExpected = list(
                map("Events.count", "4500")
        );

        addContext(mockhttpServer, countExpectedPageReachQuery,
                JsonUtil.getJsonStringFromObject(map("data", countResponseExpected)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(500, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                } else {

                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());
                }

                assertEquals(2250, variantResult.getTotalPageViews());

                Assert.assertEquals(250, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(500, variantResult.getMultiBySession());
                Assert.assertEquals(250, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                final Map<String, ResultResumeItem> details = variantResult.getDetails();
                final ResultResumeItem resultResumeItem = details.get(
                        SIMPLE_FORMATTER.format(firstEventStartDate));

                assertNotNull(resultResumeItem);

                Assert.assertEquals(250, resultResumeItem.getUniqueBySession());
                Assert.assertEquals(500, resultResumeItem.getMultiBySession());
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private static String getNotDefaultVariantName(Experiment experiment) {
        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));
        return variantName;
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D.
     * * Create a Variant inside the Experiment let call it variant_1
     * - You have the follow page_view Events for each browser sessions:
     * First Session:
     *      Variant: variant_1
     *      Date: Yesterday
     *      page View events: A, B, D and C
     *
     * Second Session:
     *   Variant: variant_1
     *   Date: Today
     *   page View events: A, B, D and C
     *
     * Third Session:
     *  Variant: DEFAULT
     *  Date: Today
     *  page View events: A, B, D and C
     *
     * Fourth Session:
     *   Variant: variant_1
     *   Date: Tomorrow
     *   page View events: A, B, D and C
     *
     * Fifth Session:
     *  Variant: DEFAULT
     *  Date: Tomorrow
     *  page View events: A, B and C
     *
     * Should:  Got the follow results
     *
     * Variant: variant_1
     * - Total Page Views: 12
     * - Unique By Session: 3
     * - Multi By Session: 3
     * - Total Sessions: 3
     * - Details:
     *     - Yesterday: 1
     *     - Today: 1
     *     - Tomorrow: 1
     *
     * Variant: Default
     * - Total Page Views: 8
     * - Unique By Session: 1
     * - Multi By Session: 1
     * - Total Sessions: 2
     * - Details:
     *     - Yesterday: 0
     *     - Today: 1
     *     - Tomorrow: 0
     */
    @Test
    public void multiDaysEvent() throws DotDataException, DotSecurityException {
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                .withZone(ZoneId.systemDefault());

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant_1 = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant today = Instant.now();
        final Instant yesterday = today.minus(1, ChronoUnit.DAYS);
        final Instant tomorrow = today.plus(1, ChronoUnit.DAYS);

        final List<Map<String, String>> yesterdayCubeJsQueryData = createPageViewEvents(yesterday,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final List<Map<String, String>> todayCubeJsQueryData = createPageViewEvents(today,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final List<Map<String, String>> todayCubeJsQueryDataDefault = createPageViewEvents(today,
                experiment, DEFAULT_VARIANT.name(), pageA, pageB, pageD, pageC);

        final List<Map<String, String>> tomorrowCubeJsQueryData = createPageViewEvents(tomorrow,
                experiment, variantName, pageA, pageB, pageD, pageC);

        final List<Map<String, String>> tomorrowCubeJsQueryDataDefault = createPageViewEvents(tomorrow,
                experiment, DEFAULT_VARIANT.name(), pageA, pageB, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data",
                concat(todayCubeJsQueryData, tomorrowCubeJsQueryDataDefault, yesterdayCubeJsQueryData,
                        tomorrowCubeJsQueryData, todayCubeJsQueryDataDefault)
        );

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);


        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "12"),
                 map("Events.variant", "DEFAULT", "Events.count", "8")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 20, mockhttpServer);
        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            final GoalResults primary = experimentResults.getGoals().get("primary");

            assertEquals(5, experimentResults.getSessions().getTotal());
            assertEquals(2, primary.getVariants().size());

            final VariantResults variantResults = primary.getVariants().get(variantName);
            final VariantResults defaultVariantResults = primary.getVariants().get(DEFAULT_VARIANT.name());

            assertEquals(12, variantResults.getTotalPageViews());
            assertEquals(3, variantResults.getUniqueBySession().getCount());
            assertEquals(3, variantResults.getMultiBySession());
            assertEquals(3, variantResults.getDetails().size());
            assertEquals(1, variantResults.getDetails().get(FORMATTER.format(yesterday)).getUniqueBySession());
            assertEquals(1, variantResults.getDetails().get(FORMATTER.format(yesterday)).getMultiBySession());
            assertEquals(1, variantResults.getDetails().get(FORMATTER.format(today)).getUniqueBySession());
            assertEquals(1, variantResults.getDetails().get(FORMATTER.format(today)).getMultiBySession());
            assertEquals(1, variantResults.getDetails().get(FORMATTER.format(tomorrow)).getUniqueBySession());
            assertEquals(1, variantResults.getDetails().get(FORMATTER.format(tomorrow)).getMultiBySession());
            assertEquals(3l, experimentResults.getSessions().getVariants().get(variantName).longValue());

            assertEquals(2, experimentResults.getSessions().getVariants().get(DEFAULT_VARIANT.name()).longValue());
            assertEquals(8, defaultVariantResults.getTotalPageViews());
            assertEquals(1, defaultVariantResults.getUniqueBySession().getCount());
            assertEquals(1, defaultVariantResults.getMultiBySession());
            assertEquals(3, defaultVariantResults.getDetails().size());
            assertEquals(0, defaultVariantResults.getDetails().get(FORMATTER.format(yesterday)).getUniqueBySession());
            assertEquals(0, defaultVariantResults.getDetails().get(FORMATTER.format(yesterday)).getMultiBySession());
            assertEquals(1, defaultVariantResults.getDetails().get(FORMATTER.format(today)).getUniqueBySession());
            assertEquals(1, defaultVariantResults.getDetails().get(FORMATTER.format(today)).getMultiBySession());
            assertEquals(0, defaultVariantResults.getDetails().get(FORMATTER.format(tomorrow)).getUniqueBySession());
            assertEquals(0, defaultVariantResults.getDetails().get(FORMATTER.format(tomorrow)).getMultiBySession());

        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private <T> List concat(final Collection<T>... collections) {
        return Stream.of(collections).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private String getTotalPageViewsQuery(final String experiment,final String... variants) {
        return "{"
                +   "\"measures\": [\"Events.count\"],"
                +   "\"dimensions\": [\"Events.variant\"],"
                + "  \"filters\": ["
                + "    {"
                + "      \"member\": \"Events.eventType\","
                + "      \"operator\": \"equals\","
                + "      \"values\": ["
                + "        \"pageview\""
                + "      ]"
                + "    },"
                + "    {"
                + "      \"member\": \"Events.variant\","
                + "      \"operator\": \"equals\","
                + "      \"values\": [" + Arrays.stream(variants).map(variant -> "\"" + variant + "\"").collect(Collectors.joining(",")) + "]"
                + "    },"
                + "    {"
                + "      \"member\": \"Events.experiment\","
                + "      \"operator\": \"equals\","
                + "      \"values\": [\"" + experiment + "\"]"
                + "    }"
                + "  ]"
                + "}";
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC, pageD, pageB );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "2")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 2, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {
                    Assert.assertEquals(1, (long) experimentResults.getSessions().getVariants().get(variantName));
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(2, variantResult.getTotalPageViews());
                } else {
                    Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);


        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "2")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 2, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(0, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(2, variantResult.getTotalPageViews());
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC, pageD );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);


        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "2")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 2, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(0, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(2, variantResult.getTotalPageViews());
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(
                pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageB, pageC, pageD);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "4")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 4, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    Assert.assertEquals(1, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(4, variantResult.getTotalPageViews());
                } else {
                    Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have 3 pages: A, B and C
     * - You create an {@link Experiment} using the B page with a BOUNCE_RATE Goal: url EQUALS TO PAge B .
     * - You have the follow page_view to the pages order by timestamp: A, C and B to the Specific Variant
     * - Also You have the follow page_view to the pages order by timestamp: A, B and C  to the Default Variant
     *
     * Should:  count 0 Minimize Bounce Rate success to the specific variant
     */
    @Test
    public void bounceRate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant(pageB);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC, pageB);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "3")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 3, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResult = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getSessions().getTotal());

            for (VariantResults variantResult : experimentResult.getGoals().get("primary").getVariants().values()) {
                final int sessionExpected = variantResult.getVariantName().equals(variantName) ? 1 : 0;

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(3, variantResult.getTotalPageViews());
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

                Assert.assertEquals(0, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(0, variantResult.getMultiBySession());
                Assert.assertEquals(sessionExpected,
                        (long) experimentResult.getSessions().getVariants()
                                .get(variantResult.getVariantName()));

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

    @DataProvider
    public static String[] indexPaths() throws Exception {
        return new String[]{
                "/index", "/INDEX", "/IndEX", "", "/", "/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true"
        };
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have 3 pages: A, B and Index
     * - You create an {@link Experiment} using the Index page with a BOUNCE_RATE Goal: url EQUALS TO PAge Index .
     * - You have the follow page_view to the pages order by timestamp: A, Index and C to the Specific Variant
     *
     * Should:  count 1 Minimize Bounce Rate success to the specific variant
     */
    @Test
    @UseDataProvider("indexPaths")
    public void bounceRateWithIndex(final String indexPath) throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageIndex = new HTMLPageDataGen(host, template).pageURL("index").nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .build();

        final Experiment experiment = createExperiment(pageIndex, metric);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();

        final String[] pagesUrl = new String[]{
                pageA.getURI(),
                indexPath,
                pageC.getURI()};

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pagesUrl);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "3")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 3, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResult = experimentsAPIImpl.getResults(
                experiment,
                APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getSessions().getTotal());

            for (VariantResults variantResult : experimentResult.getGoals().get("primary").getVariants().values()) {
                final int sessionExpected = variantResult.getVariantName().equals(variantName) ? 1 : 0;

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(3, variantResult.getTotalPageViews());
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

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
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment, User)}
     * When:
     * - You have 3 pages: A, B and Index
     * - You create an {@link Experiment} using the Index page with a BOUNCE_RATE Goal: url EQUALS TO PAge Index .
     * - You have the follow page_view to the pages order by timestamp: A, C and Index  to the Specific Variant
     *
     * Should:  count 0 Minimize Bounce Rate success to the specific variant
     */
    @Test
    @UseDataProvider("indexPaths")
    public void noBounceRateWithIndex(final String indexPath) throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageIndex = new HTMLPageDataGen(host, template).pageURL("index").nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .build();

        final Experiment experiment = createExperiment(pageIndex, metric);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();

        final String[] pagesUrl = new String[]{
                pageA.getURI(),
                pageC.getURI(),
                indexPath};

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pagesUrl);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "3")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 3, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentResults experimentResult = experimentsAPIImpl.getResults(
                experiment,
                APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getSessions().getTotal());

            for (VariantResults variantResult : experimentResult.getGoals().get("primary").getVariants().values()) {
                final int sessionExpected =  0;

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(3, variantResult.getTotalPageViews());

                    Assert.assertEquals(1,
                            (long) experimentResult.getSessions().getVariants()
                                    .get(variantResult.getVariantName()));
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());

                    Assert.assertEquals(0,
                            (long) experimentResult.getSessions().getVariants()
                                    .get(variantResult.getVariantName()));
                }

                Assert.assertEquals(sessionExpected, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(sessionExpected, variantResult.getMultiBySession());


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
     * - You have the follow page_view to the pages order by timestamp: A, B and  C
     *
     * Should:  count 1 Minimize Bounce Rate success to the specific variant
     */
    @Test
    public void notBounceRate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant(pageB);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageB, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "3")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 3, mockhttpServer);
        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                final int sessionExpected = variantResult.getVariantName().equals(variantName) ? 1 : 0;

                if (variantResult.getVariantName().equals(variantName)) {
                    Assert.assertEquals(1, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(3, variantResult.getTotalPageViews());
                } else {
                    Assert.assertEquals(0, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

                Assert.assertEquals(sessionExpected, variantResult.getUniqueBySession().getCount());
                Assert.assertEquals(sessionExpected, variantResult.getMultiBySession());

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant(pageB);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pageA, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "2")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 2, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResult = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(0, experimentResult.getSessions().getTotal());

            for (VariantResults variantResult : experimentResult.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(2, variantResult.getTotalPageViews());
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageB, pageD);

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

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

        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "4")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 4, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

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

                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(4, variantResult.getTotalPageViews());
                } else {
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private static void addCountQueryContext(final Experiment experiment, final int count,
            final MockHttpServer mockhttpServer) {
        final String countExpectedPageReachQuery = getCountExpectedPageReachQuery(experiment);
        final List<Map<String, Object>> countResponseExpected = list(
                map("Events.count", String.valueOf(count))
        );

        addContext(mockhttpServer, countExpectedPageReachQuery,
                JsonUtil.getJsonStringFromObject(map("data", countResponseExpected)));
    }

    private Experiment createExperimentWithBounceRateGoalAndVariant(final HTMLPageAsset experimentPage) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .addConditions(getUrlCondition(experimentPage.getPageUrl()))
                .build();

        return createExperiment(experimentPage, metric);
    }

    private List<Map<String, String>> createPageViewEvents(final Instant firstEventTime,
            final Experiment experiment,
            final String variantName,
            final HTMLPageAsset... pages) throws DotDataException {
        return createPageViewEvents(firstEventTime, experiment, variantName, Arrays.stream(pages)
                .map(page -> "/" + page.getPageUrl())
                .toArray(String[]::new));
    }
    private List<Map<String, String>> createPageViewEvents(final Instant firstEventTime,
        final Experiment experiment,
        final String variantName,
        final String... pagesUrl) {

        final String lookBackWindows = new RandomString(20).nextString();
        final List<Map<String, String>> dataList = new ArrayList<>();
        Instant nextEventTriggerTime = firstEventTime;
        String previousPageUrl = StringPool.BLANK;

        for (final String pageUrl : pagesUrl) {
            dataList.add(map(
                    "Events.experiment", experiment.getIdentifier(),
                    "Events.variant", variantName,
                    "Events.utcTime", EVENTS_FORMATTER.format(nextEventTriggerTime),
                    "Events.referer", previousPageUrl,
                    "Events.url", "http://127.0.0.1:5000" + pageUrl,
                    "Events.lookBackWindow", lookBackWindows,
                    "Events.eventType", EventType.PAGE_VIEW.getName()
            ));

            nextEventTriggerTime = nextEventTriggerTime.plus(1, ChronoUnit.MILLIS);
            previousPageUrl = pageUrl;
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


    private static String getExpectedPageReachQuery(final Experiment experiment, final long limit,
            final long offset) {
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
                +   "},"
                +   "\"limit\":" + limit + ","
                +   "\"offset\":" + offset
                + "}";
        return cubeJSQueryExpected;
    }

    private static String getCountExpectedPageReachQuery(final Experiment experiment) {
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
                +   "\"measures\":["
                +       "\"Events.count\""
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
        final AnalyticsProperties analyticsProperties = AnalyticsProperties.builder()
                .clientId("clientId")
                .clientSecret("clientSecret")
                .analyticsKey("analyticsKey")
                .analyticsConfigUrl("http://localhost:8088/c/customer1/cluster1/keys")
                .analyticsWriteUrl("http://localhost:8081/api/v1/event")
                .analyticsReadUrl(CUBEJS_SERVER_URL)
                .build();

        final AnalyticsApp mockAnalyticsApp = mock(AnalyticsApp.class);
        when(mockAnalyticsApp.getAnalyticsProperties()).thenReturn(analyticsProperties);

        final AnalyticsHelper mockAnalyticsHelper = mock(AnalyticsHelper.class);
        when(mockAnalyticsHelper.appFromHost(currentHost)).thenReturn(mockAnalyticsApp);

        return mockAnalyticsHelper;
    }

    private static AnalyticsHelper mockInvalidAnalyticsHelper() throws DotDataException, DotSecurityException {
        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
        final AnalyticsProperties analyticsProperties = AnalyticsProperties.builder()
            .clientId("clientId")
            .clientSecret("clientSecret")
            .analyticsKey("analyticsKey")
            .analyticsConfigUrl("http://localhost:8088/c/customer1/cluster1/keys")
            .analyticsWriteUrl("http://localhost:8081/api/v1/event")
            .analyticsReadUrl(CUBEJS_SERVER_URL)
            .build();

        final AnalyticsApp mockAnalyticsApp = mock(AnalyticsApp.class);
        when(mockAnalyticsApp.getAnalyticsProperties()).thenReturn(analyticsProperties);

        final AnalyticsHelper mockAnalyticsHelper = mock(AnalyticsHelper.class);
        when(mockAnalyticsHelper.appFromHost(currentHost)).thenThrow(new IllegalStateException("Error!"));

        return mockAnalyticsHelper;
    }

    private static void addContext(final MockHttpServer mockHttpServer,
            final String expectedQuery, final String responseBody) {

        final MockHttpServerContext mockHttpServerContext = new  MockHttpServerContext.Builder()
                .uri("/cubejs-api/v1/load")
                .requestCondition((requestContext) ->
                        String.format( "Cube JS Query is not right, \nExpected: %s \nCurrent %s",
                                expectedQuery, requestContext.getRequestParameter("query")
                                        .orElse(StringPool.BLANK)),
                        requestContext -> isEquals(expectedQuery, requestContext))
                .responseStatus(HttpURLConnection.HTTP_OK)
                .mustBeCalled()
                .responseBody(responseBody)
                .build();

        mockHttpServer.addContext(mockHttpServerContext);
    }

    private static boolean isEquals(final String expectedQuery, final RequestContext context) {
        try {
            return context.getRequestParameterAsMap("query")
                    .orElse(Collections.emptyMap())
                    .equals(JsonUtil.getJsonFromString(expectedQuery));
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static MockHttpServer createMockHttpServer(final String expectedQuery, final String responseBody) {
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

        return mockhttpServer;
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Saving an Experiment
     * Should: set the Url parameter automatically with a CONTAINS condition for the url of the page to reach
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void addUrlConditionToReachPageGoal() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

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
        final ImmutableList<Condition> conditions = goals.primary().getMetric().conditions();

        assertEquals(1, conditions.size());

        final Condition condition = conditions.get(0);
        assertEquals("url", condition.parameter());
        assertEquals(reachPage.getPageUrl(), condition.value());
        assertEquals(Operator.CONTAINS, condition.operator());
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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
        APILocator.getExperimentsAPI().getResults(experiment, APILocator.systemUser());
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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
        APILocator.getExperimentsAPI().getResults(experiment, APILocator.systemUser());
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

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

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
        final ImmutableList<Condition> conditions = goals.primary().getMetric().conditions();

        assertEquals(1,  conditions.size());

        assertEquals("url", conditions.get(0).parameter());
        assertEquals("^(http|https):\\/\\/(localhost|127.0.0.1|\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,})(:\\d{1,5})?\\" + experimentPage.getURI() + "(\\/?\\?.*)?$",
                conditions.get(0).value());
        assertEquals(Operator.REGEX, conditions.get(0).operator());
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

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

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
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment, User)}
     * When:
     * - Create 4 pages,: a, B, C and D.
     * - Create a content type with a text field and pageB as detail page,
     * and urlMapper equals to: /testpattern/{field}
     * - Create a new Contentlet with the content type created above and the field value equals to: test
     * - Create pageview Events for the pages: pageA's url, "/testpattern/test", pageC's Url and pageD's Url
     *
     * Should: count one session and one goal success.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void resultWithUrlMap() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();

        final Field field = new FieldDataGen().next();

        final String prefixUrlMapper = "/testpattern" + System.currentTimeMillis() + "/";
        final String urlMapper = prefixUrlMapper + "{" + field.variable() + "}";

        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(pageB.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();

        new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "test")
                .nextPersisted();

        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(
                        getUrlCondition(pageD.getPageUrl()))
                .build();

        final Experiment experiment = createExperiment(pageB, metric, new String[]{RandomString.make(15)});

        final String variantName = getNotDefaultVariantName(experiment);
        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Instant firstEventStartDate = Instant.now();
        final String[] pagesUrl = new String[]{pageA.getPageUrl(),
                prefixUrlMapper + "test",
                pageD.getURI(),
                pageC.getURI()};

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(firstEventStartDate,
                experiment, variantName, pagesUrl);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);
        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "4")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 4, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();

            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                experiment,
                APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(1, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(variantName)) {
                    assertEquals(variant.description().get(), variantResult.getVariantDescription());
                    assertEquals(4, variantResult.getTotalPageViews());
                } else {

                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());
                    assertEquals(0, variantResult.getTotalPageViews());
                }

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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageA, pageC);
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

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "20")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 20, mockhttpServer);
        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());
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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have three pages: A, B and C
     * - You create an {@link Experiment} using the A page with a Bounce Rate Goal
     * - You have the follow page_view Event in Different Sessions:
     *
     * Session 1 : A, this is a Bounce Rate.
     * Session 2: A, B, This is not a Bounce Rate.
     *
     * Should: calculate the probability that B beats A is 0.99
     */
    @Test
    public void test_calcBayesian_BOverA_BounceRate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .addConditions(getUrlCondition(pageA.getPageUrl()))
                .build();

        final Experiment experiment = createExperiment(pageA, metric, RandomString.make(15));
        final String variantName = getNotDefaultVariantName(experiment);

        final List<Map<String, String>> pageViewEvents_1 = createPageViewEvents(Instant.now(),
                experiment, variantName, pageA);

        final List<Map<String, String>> pageViewEvents_2 = createPageViewEvents(Instant.now(),
                experiment, variantName, pageA, pageB);

        final List<Map<String, String>> pageViewEvents_3 = createPageViewEvents(Instant.now(),
                experiment, "DEFAULT", pageA, pageB);

        final List<Map<String, String>> data = new ArrayList<>(pageViewEvents_1);
        data.addAll(pageViewEvents_2);
        data.addAll(pageViewEvents_3);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult = map("data", data);

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "3")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 3, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());
            assertEquals(3, experimentResults.getSessions().getTotal());

            final BayesianResult bayesianResult = experimentResults.getBayesianResult();
            Assert.assertEquals(bayesianResult.suggestedWinner(), "DEFAULT");

            mockhttpServer.validate();
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageA, pageC);
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

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantName, "Events.count", "50")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 50, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());
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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageA, pageC);
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

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
            map("Events.variant", variantName, "Events.count", "50")
        );

        addContext(mockhttpServer, queryTotalPageViews,
            JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 50, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);

            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());
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
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
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
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageA, pageC, "variantB", "variantC");
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

        final MockHttpServer mockhttpServer = new MockHttpServer(CUBEJS_SERVER_IP, CUBEJS_SERVER_PORT);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final String queryTotalPageViews = getTotalPageViewsQuery(experiment.id().get(), "DEFAULT", variantBName, variantCName);

        final List<Map<String, Object>> totalPageViewsResponseExpected = list(
                map("Events.variant", variantBName, "Events.count", "50"),
                map("Events.variant", variantCName, "Events.count", "50")
        );

        addContext(mockhttpServer, queryTotalPageViews,
                JsonUtil.getJsonStringFromObject(map("data", totalPageViewsResponseExpected)));

        addCountQueryContext(experiment, 100, mockhttpServer);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper();
            ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());
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

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When:
     * - You have a set of pages that are required to create an experiment
     * And:
     * - A missing analytics application
     *
     * Should: thrown an exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_getResults_missingAnalyticsApp() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(pageA, pageC, "variantB", "variantC");
        final AnalyticsHelper mockAnalyticsHelper = mockInvalidAnalyticsHelper();
        ExperimentAnalyzerUtil.setAnalyticsHelper(mockAnalyticsHelper);
        final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
        experimentsAPIImpl.getResults(experiment, APILocator.systemUser());
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResults(Experiment, User)}
     * When:
     * - Create a Page.
     * - Create a content type with a text field and newly created page as detail page,
     * and urlMapper equals to: /testpattern/{field}
     * - Create a new Contentlet with the content type created above and the field value equals to: test
     * - Create and start a Experiment using the newly created page and one no DEFAULT Variant, the
     * Goal of the Experiment is a PAGE_REACHbut it doed not really matter.
     * - Try to render the page in LIVE mode and the no Default Variant of the Experiment
     *
     * Should: Not got a {@link DotSecurityException}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void renderUrlMapPageWithExperiment() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Field field = new FieldDataGen().next();

        final String prefixUrlMapper = "/testpattern" + System.currentTimeMillis() + "/";
        final String urlMapper = prefixUrlMapper + "{" + field.variable() + "}";

        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(experimentPage.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();

        new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "test")
                .nextPersistedAndPublish();

        final HTMLPageAsset reachPageTarget = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(
                        getUrlCondition(reachPageTarget.getPageUrl()))
                .build();

        final Experiment experiment = createExperiment(experimentPage, metric, new String[]{RandomString.make(15)});

        final String variantName = getNotDefaultVariantName(experiment);
        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURI()).thenReturn(prefixUrlMapper + "test");

        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variantName);
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);

        final HttpServletResponse response = mock(HttpServletResponse.class);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        try {
            APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setPageUri(prefixUrlMapper + "test")
                            .setUser(APILocator.getUserAPI().getAnonymousUser())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    request,
                    response
            );
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());


        }
    }

}
