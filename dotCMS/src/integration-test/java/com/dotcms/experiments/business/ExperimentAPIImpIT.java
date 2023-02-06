package com.dotcms.experiments.business;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.analytics.app.AnalyticsApp;
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
import com.dotcms.experiments.business.result.ExperimentResult;
import com.dotcms.experiments.business.result.ExperimentResult.GoalResult;
import com.dotcms.experiments.business.result.ExperimentResult.VariantResult;
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
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.bytebuddy.utility.RandomString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link ExperimentsAPIImpl}
 */
public class ExperimentAPIImpIT {

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

            List<String> experiemtnsId = experimentRunning.stream()
                    .map(experiment -> experiment.getIdentifier()).collect(Collectors.toList());

            assertTrue(experiemtnsId.contains(runningExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(draftExperiment.getIdentifier()));
            assertTrue(experiemtnsId.contains(stoppedExperiment.getIdentifier()));

            ExperimentDataGen.end(stoppedExperiment);

            experimentRunning = APILocator.getExperimentsAPI()
                    .getRunningExperiments();
            experiemtnsId = experimentRunning.stream()
                    .map(experiment -> experiment.getIdentifier()).collect(Collectors.toList());

            assertTrue(experiemtnsId.contains(runningExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(draftExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(stoppedExperiment.getIdentifier()));
        } finally {
            ExperimentDataGen.end(runningExperiment);

            final Optional<Experiment> experiment = APILocator.getExperimentsAPI()
                    .find(stoppedExperiment.getIdentifier(), APILocator.systemUser());

            if (experiment.isPresent() && experiment.get().status() == Status.RUNNING) {
                ExperimentDataGen.end(experiment.get());
            }
        }
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
     * When: Call the methods with a Experiment with 4 Session each one with several
     * {@link com.dotcms.analytics.metrics.EventType#PAGE_VIEW} events.
     * Should: get all the Events group by lookBackWindow
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getEvents() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final String variantName_1 = "experiment_page_reach+testing_1_variant_1";
        final String variantName_2 = "experiment_page_reach+testing_1_variant_2";

        final Experiment experiment = createExperimentWithGoalAndVariant("experiment_events_testing_1",
                pageB, pageD);

        final List<Map<String, String>> events_1 = createPageViewEvents(experiment, variantName_1, pageA,
                pageB, pageD, pageC);

        final List<Map<String, String>> events_2 = createPageViewEvents(experiment, variantName_1, pageD, pageC);

        final List<Map<String, String>> events_3 = createPageViewEvents(experiment, variantName_2, pageA, pageB);

        final List<Map<String, String>> events_4 = createPageViewEvents(experiment, variantName_2, pageA,
                pageB, pageD, pageC);

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

        final MockHttpServer mockHttpServer = createMockHttpServerAndStart(cubeServerIp,
                cubeJsServerPort, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        IPUtils.disabledIpPrivateSubnet(true);

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));

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

    private static Experiment createExperimentWithGoalAndVariant(final String experimentName,
             final HTMLPageAsset pageB, final HTMLPageAsset pageD) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(pageD.getPageUrl()),
                        getRefererCondition(pageB.getPageUrl()))
                .build();



        final Goals goal = Goals.builder().primary(metric).build();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant(experimentName)
                .page(pageB)
                .addVariant("description")
                .addGoal(goal)
                .nextPersisted();
        return experiment;
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
     * Method to test: {@link ExperimentsAPIImpl#getResult(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, B, D and C
     *
     * Should:  get a Success PAGE_REACH
     */
    @Test
    public void pageReachDirectReferer() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(experiment, variantName, pageA,
                pageB, pageD, pageC);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(cubeServerIp,
                cubeJsServerPort, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResult experimentResult = experimentsAPIImpl.getResult(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getTotalSessions());

            for (VariantResult variantResult : experimentResult.getGoalResults().get(0).getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {

                    Assert.assertEquals(1, variantResult.totalUniqueBySession());
                    Assert.assertEquals(1, variantResult.totalMultiBySession());
                } else {
                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResult(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, C, D and B
     *
     * Should:  get a Failed PAGE_REACH
     */
    @Test
    public void pageReachNotDirectReferer() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(experiment, variantName, pageA,
                pageC, pageD, pageB );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(cubeServerIp,
                cubeJsServerPort, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResult experimentResult = experimentsAPIImpl.getResult(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getTotalSessions());

            for (VariantResult variantResult : experimentResult.getGoalResults().get(0).getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {

                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                } else {
                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResult(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A and C
     *
     * Should:  get a Failed PAGE_REACH
     */
    @Test
    public void pageNotReachButExperimentPageWasView() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(experiment, variantName, pageA,
                pageC );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(cubeServerIp,
                cubeJsServerPort, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResult experimentResult = experimentsAPIImpl.getResult(experiment);

            mockhttpServer.validate();

            assertEquals(0, experimentResult.getTotalSessions());

            for (VariantResult variantResult : experimentResult.getGoalResults().get(0).getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {

                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                } else {
                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResult(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, C and D
     *
     * Should:  get a Failed PAGE_REACH
     */
    @Test
    public void pageReachButExperimentPageWasNotView() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(experiment, variantName, pageA,
                pageC, pageD );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(cubeServerIp,
                cubeJsServerPort, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResult experimentResult = experimentsAPIImpl.getResult(experiment);

            mockhttpServer.validate();

            assertEquals(0, experimentResult.getTotalSessions());

            for (VariantResult variantResult : experimentResult.getGoalResults().get(0).getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {

                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                } else {
                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getResult(Experiment)}
     * When:
     * - You have four pages: A, B, C and D
     * - You create an {@link Experiment} using the B page with a PAGE_REACH Goal: url EQUALS TO PAge D .
     * - You have the follow page_view to the pages order by timestamp: A, B, C and D
     *
     * Should:  get a Faliled PAGE_REACH
     */
    @Test
    public void pageReachAfterExperimentPageButNotDirectReferer() throws DotDataException, DotSecurityException {
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageC = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageD = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithGoalAndVariant("experiment_page_reach_testing_1",
                pageB, pageD);

        final String variantName = experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));

        final List<Map<String, String>> cubeJsQueryData = createPageViewEvents(experiment, variantName, pageA,
                pageB, pageC, pageD);

        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  map("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        final MockHttpServer mockhttpServer = createMockHttpServerAndStart(cubeServerIp,
                cubeJsServerPort, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        try {
            final AnalyticsHelper mockAnalyticsHelper = mockAnalyticsHelper(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl(mockAnalyticsHelper);
            final ExperimentResult experimentResult = experimentsAPIImpl.getResult(experiment);

            mockhttpServer.validate();

            assertEquals(1, experimentResult.getTotalSessions());

            for (VariantResult variantResult : experimentResult.getGoalResults().get(0).getVariants().values()) {
                if (variantResult.getVariantName().equals(variantName)) {

                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                } else {
                    Assert.assertEquals(0, variantResult.totalUniqueBySession());
                    Assert.assertEquals(0, variantResult.totalMultiBySession());
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private List<Map<String, String>> createPageViewEvents(final Experiment experiment,
            final String variantName,
            final HTMLPageAsset... pages) {

        final String lookBackWindows = new RandomString(20).nextString();
        final List<Map<String, String>> dataList = new ArrayList<>();
        Instant nextEventTriggerTime = Instant.now();
        HTMLPageAsset previousPage = null;

        for (final HTMLPageAsset page : pages) {
            dataList.add(map(
                    "Events.experiment", experiment.getIdentifier(),
                    "Events.variant", variantName,
                    "Events.utcTime", nextEventTriggerTime.toString(),
                    "Events.referer", UtilMethods.isSet(previousPage) ?
                            "http://127.0.0.1:5000/" + previousPage.getPageUrl() : StringPool.BLANK,
                    "Events.url", "http://127.0.0.1:5000/" + page.getPageUrl(),
                    "Events.lookBackWindow", lookBackWindows,
                    "Events.eventType", EventType.PAGE_VIEW.getName()
            ));

            nextEventTriggerTime = nextEventTriggerTime.plus(1, ChronoUnit.MILLIS);
            previousPage = page;
        }
        return dataList;
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

    private static AnalyticsHelper mockAnalyticsHelper(final String analyticsReadUrl)
            throws DotDataException, DotSecurityException {

        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();

        final AbstractAnalyticsProperties mockAnalyticsProperties = mock(AbstractAnalyticsProperties.class);
        when(mockAnalyticsProperties.analyticsReadUrl())
                .thenReturn(analyticsReadUrl);

        final AnalyticsApp mockAnalyticsApp = mock(AnalyticsApp.class);
        when(mockAnalyticsApp.getAnalyticsProperties()).thenReturn(mockAnalyticsProperties);

        final AnalyticsHelper mockAnalyticsHelper = mock(AnalyticsHelper.class);
        when(mockAnalyticsHelper.appFromHost(currentHost)).thenReturn(mockAnalyticsApp);
        return mockAnalyticsHelper;
    }

    private static MockHttpServer createMockHttpServerAndStart(final String cubeServerIp,
            final int cubeJsServerPort, final String expectedQuery, final String responseBody) {
        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

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

    //get result to a Experiment not started
}
