package com.dotcms.experiments.business;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.analytics.metrics.QueryParameter;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.cube.CubeJSClientFactoryImpl;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.NotAllowedException;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.VariantResults;
import com.dotcms.experiments.business.result.VariantResults.ResultResumeItem;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.AbstractTrafficProportion.Type;
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
import com.dotcms.util.WireMockTestHelper;
import com.dotcms.util.network.IPUtils;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import net.bytebuddy.utility.RandomString;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ExperimentsAPIImpl}
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ExperimentAPIImpIntegrationTest extends IntegrationTestBase {

    private static final String MOCK_SERVER_IP = "127.0.0.1";
    private static final int MOCK_SERVER_PORT = 5000;
    private static final DateTimeFormatter EVENTS_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
            .withZone(ZoneId.systemDefault());
    private static final int PORT = 50505;
    private static WireMockServer wireMockServer;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        wireMockServer = prepareWireMock();
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
    }

    private static WireMockServer prepareWireMock() {
        final WireMockServer wireMockServer = WireMockTestHelper.wireMockServer(PORT);
        wireMockServer.start();

        return wireMockServer;
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
                .status(Status.DRAFT)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        final Experiment experimentFromDataBase_1 = APILocator.getExperimentsAPI()
                .find(experiment.id().get(), APILocator.systemUser())
                .orElseThrow(() -> new AssertionError("Experiment not found"));

        assertNotNull(experimentFromDataBase_1.runningIds());

        assertFalse(experimentFromDataBase_1.runningIds().iterator().hasNext());

        final Experiment scheduledExperiment = APILocator.getExperimentsAPI()
                .start(experiment.id().get(), APILocator.systemUser());

        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .startScheduled(scheduledExperiment.id().get(), APILocator.systemUser());

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

        final RunningId firstRunningId = experimentStarted.runningIds().get(0);

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

            final RunningId currentRunningId = experimentAfterReStart.runningIds().getCurrent().orElseThrow();

            assertNotEquals(firstRunningId.id(), currentRunningId.id());
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
                .status(Status.DRAFT)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        final Experiment scheduledExperiment = APILocator.getExperimentsAPI()
                .start(experiment.id().get(), APILocator.systemUser());

        final Experiment startedExperiment = APILocator.getExperimentsAPI()
                .startScheduled(scheduledExperiment.id().get(), APILocator.systemUser());

        final Experiment endedExperiment = APILocator.getExperimentsAPI()
                .end(startedExperiment.id().get(), APILocator.systemUser());

        final Experiment experimentToRestart = APILocator.getExperimentsAPI()
                .save(
                        Experiment.builder().from(endedExperiment)
                        .status(Status.DRAFT)
                        .scheduling(Scheduling.builder()
                                .startDate(Instant.now())
                                .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                                .build())
                        .build(), APILocator.systemUser());

        final Experiment rescheduledExperiment = APILocator.getExperimentsAPI()
                .start(experimentToRestart.id().get(), APILocator.systemUser());

        final Experiment restartedExperiment = APILocator.getExperimentsAPI()
                .startScheduled(rescheduledExperiment.id().get(), APILocator.systemUser());

        try {

            assertEquals(2, restartedExperiment.runningIds().size());

            assertTrue(restartedExperiment.runningIds().getAll().stream()
                    .anyMatch(runningId -> runningId.endDate() != null));

            assertTrue(restartedExperiment.runningIds().getAll().stream()
                    .anyMatch(runningId -> runningId.endDate() == null));

            assertNotSame(restartedExperiment.runningIds().get(0).id(),
                    restartedExperiment.runningIds().get(1).id());
        } finally {
            APILocator.getExperimentsAPI().end(experimentToRestart.id().get(), APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPI#getRunningExperiments()}
     * When: You have tree Experiment:
     * - First one in DRAFT state, it has never been running.
     * - Second and third are started.
     *
     * Should : The method return the second and third Experiment,
     * Also before called the method the Running Experiment cache must be empty and after called the method the cache
     * must include the two Running Experiment.
     *
     * later Stop the third Experiment, and called the method again, now the method
     * must return just the Second Experiment.
     * Also, the Running Experiment Cache must ve empty before called the methos and include just the second Experiment
     * after call it.
     */
    @Test
    public void getRunningExperiment() throws DotDataException, DotSecurityException {
        final Experiment draftExperiment = new ExperimentDataGen().nextPersisted();

        final Experiment runningExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(runningExperiment);

        final Experiment stoppedExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(stoppedExperiment);

        assertCachedRunningExperiments(Collections.emptyList());

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

            assertCachedRunningExperiments(Collections.emptyList());

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

    /**
     * Method to test: {@link ExperimentsAPI#getRunningExperiments()}
     * When: You have 2 Experiments running on Site Aand 1 more running on Site B
     * Should: return the Experiment to the Specific SIte
     */
    @Test
    public void getRunningExperimentByHost() throws DotDataException, DotSecurityException {
        final Host siteA = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();

        final HTMLPageAsset pageA1 = new HTMLPageDataGen(siteA, template).nextPersisted();

        final HTMLPageAsset pageA2 = new HTMLPageDataGen(siteA, template).nextPersisted();

        final Host siteB = new SiteDataGen().nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(siteB, template).nextPersisted();

        final Experiment experimentA1 = new ExperimentDataGen().page(pageA1).nextPersisted();
        APILocator.getExperimentsAPI().start(experimentA1.id().get(), APILocator.systemUser());

        final Experiment experimentA2 = new ExperimentDataGen().page(pageA2).nextPersisted();
        APILocator.getExperimentsAPI().start(experimentA2.id().get(), APILocator.systemUser());

        final Experiment experimentB = new ExperimentDataGen().page(pageB).nextPersisted();
        APILocator.getExperimentsAPI().start(experimentB.id().get(), APILocator.systemUser());

        try {
            final List<String> experimentsRunninsAllSites = APILocator.getExperimentsAPI()
                    .getRunningExperiments().stream()
                    .map(experiment -> experiment.id().get())
                    .collect(Collectors.toList());

            assertEquals(3, experimentsRunninsAllSites.size());
            assertTrue(experimentsRunninsAllSites.containsAll(list(experimentA1.id().get(), experimentA2.id().get(),
                    experimentB.id().get())));

            final List<String> experimentsRunninsSiteA = APILocator.getExperimentsAPI()
                    .getRunningExperiments(siteA).stream()
                    .map(experiment -> experiment.id().get())
                    .collect(Collectors.toList());

            assertEquals(2, experimentsRunninsSiteA.size());
            assertTrue(experimentsRunninsSiteA.containsAll(list(experimentA1.id().get(), experimentA2.id().get())));

            final List<String> experimentsRunninsSiteB = APILocator.getExperimentsAPI()
                    .getRunningExperiments(siteB).stream()
                    .map(experiment -> experiment.id().get())
                    .collect(Collectors.toList());

            assertEquals(1, experimentsRunninsSiteB.size());
            assertEquals(experimentB.id().get(), experimentsRunninsSiteB.get(0));
        } finally {
            APILocator.getExperimentsAPI().end(experimentA1.id().get(), APILocator.systemUser());
            APILocator.getExperimentsAPI().end(experimentA2.id().get(), APILocator.systemUser());
            APILocator.getExperimentsAPI().end(experimentB.id().get(), APILocator.systemUser());
        }
    }

    private static void assertCachedRunningExperiments(final List<String> experimentIds) {
        final List<Experiment> runningExperimentsCached = CacheLocator.getExperimentsCache()
                .getList(ExperimentsCache.CACHED_EXPERIMENTS_KEY);

        if (runningExperimentsCached != null) {
            assertEquals(experimentIds.size(), runningExperimentsCached.size());
            runningExperimentsCached.forEach(experiment -> assertTrue(experimentIds.contains(experiment.getIdentifier())));
        } else {
            assertTrue(experimentIds == null || experimentIds.isEmpty());
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

    private static Experiment createExperimentWithReachPageGoalAndVariant(final HTMLPageAsset experimentPage,
            final HTMLPageAsset reachPage) {

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();
        return createExperiment(experimentPage, metric, new String[]{RandomString.make(15)});
    }

    private static Experiment createExperimentWithReachPageGoalAndVariant(final HTMLPageAsset experimentPage,
            final HTMLPageAsset reachPage,
            final String... variants) {

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
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

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
        setAnalyticsHelper(mockAnalyticsHelper);

        final Experiment experimentStarted = ExperimentDataGen.start(experiment);

        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(Map.of("data", Collections.EMPTY_LIST)));

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", Collections.EMPTY_LIST)));

        try {

            final String noDefaultVariantName = experiment.trafficProportion().variants().stream()
                    .map(experimentVariant -> experimentVariant.id())
                    .filter(id -> !id.equals("DEFAULT"))
                    .findFirst()
                    .orElseThrow();

            final ExperimentsAPIImpl experimentsAPI = new ExperimentsAPIImpl();

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
            ExperimentDataGen.end(experimentStarted);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When: You have a REACH Page Experiment and try to get the results
     * Should: Send the right query to CubeJS Server and responding the right Result
     */
    @Test
    public void reachPageGoalResults() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset targetPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithReachPageGoalAndVariant(experimentPage, targetPage);

        final String experimentNoDefaultVariantName = getNotDefaultVariantName(experiment);
        final Variant experimentNoDefaultVariant = APILocator.getVariantAPI().get(experimentNoDefaultVariantName).orElseThrow();

        final List<Map<String, String>> cubeJsQueryData = list(
                Map.of(
                    "Events.variant", experimentNoDefaultVariantName,
                    "Events.day.day", "2023-10-27T00:00:00.000",
                    "Events.day", "2023-10-27T00:00:00.000",
                    "Events.totalSessions", "2",
                    "Events.targetVisitedAfterSuccesses", "1",
                    "Events.targetVisitedAfterConvertionRate", "50"
                ),
                Map.of(
                    "Events.variant", "DEFAULT",
                    "Events.day.day", "2023-10-27T00:00:00.000",
                    "Events.day", "2023-10-27T00:00:00.000",
                    "Events.totalSessions", "1",
                    "Events.targetVisitedAfterSuccesses", "0",
                    "Events.targetVisitedAfterConvertionRate", "0"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", "2023-10-28T00:00:00.000",
                        "Events.day", "2023-10-28T00:00:00.000",
                        "Events.totalSessions", "3",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "33"
                ),
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.day.day", "2023-10-29T00:00:00.000",
                        "Events.day", "2023-10-29T00:00:00.000",
                        "Events.totalSessions", "4",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "25"
                )
        );
        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  Map.of("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.totalSessions", "4",
                        "Events.targetVisitedAfterSuccesses", "2",
                        "Events.targetVisitedAfterConvertionRate", "50"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "3",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "33"
                )
        );

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();

            setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(7, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(experimentNoDefaultVariantName)) {
                    assertEquals(experimentNoDefaultVariant.description().get(), variantResult.getVariantDescription());

                    Assert.assertEquals(2, variantResult.getUniqueBySession().getCount());
                    Assert.assertEquals(50.0f, variantResult.getUniqueBySession().getConversionRate(), 0);
                    Assert.assertEquals(4, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    final Map<String, ResultResumeItem> details = variantResult.getDetails();
                    assertEquals(3, details.size());

                    final Iterator<Map.Entry<String, ResultResumeItem>> iterator = details.entrySet().iterator();

                    final Map.Entry<String, ResultResumeItem> first = iterator.next();
                    assertEquals("2023-10-27", first.getKey());

                    assertEquals(2, first.getValue().getTotalSessions());
                    assertEquals(1, first.getValue().getUniqueBySession());
                    assertEquals(50f, first.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> second = iterator.next();
                    assertEquals("2023-10-28", second.getKey());

                    assertEquals(0, second.getValue().getTotalSessions());
                    assertEquals(0, second.getValue().getUniqueBySession());
                    assertEquals(0f, second.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> third = iterator.next();
                    assertEquals("2023-10-29", third.getKey());

                    assertEquals(4, third.getValue().getTotalSessions());
                    assertEquals(1, third.getValue().getUniqueBySession());
                    assertEquals(25f, third.getValue().getConversionRate(), 0);
                } else {
                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());

                    Assert.assertEquals(1, variantResult.getUniqueBySession().getCount());
                    Assert.assertEquals(33.0f, variantResult.getUniqueBySession().getConversionRate(), 0);
                    Assert.assertEquals(3, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    final Map<String, ResultResumeItem> details = variantResult.getDetails();
                    assertEquals(3, details.size());


                    final Iterator<Map.Entry<String, ResultResumeItem>> iterator = details.entrySet().iterator();

                    final Map.Entry<String, ResultResumeItem> first = iterator.next();
                    assertEquals("2023-10-27", first.getKey());

                    assertEquals(1, first.getValue().getTotalSessions());
                    assertEquals(0, first.getValue().getUniqueBySession());
                    assertEquals(0f, first.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> second = iterator.next();
                    assertEquals("2023-10-28", second.getKey());

                    assertEquals(3, second.getValue().getTotalSessions());
                    assertEquals(1, second.getValue().getUniqueBySession());
                    assertEquals(33f, second.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> third = iterator.next();
                    assertEquals("2023-10-29", third.getKey());

                    assertEquals(0, third.getValue().getTotalSessions());
                    assertEquals(0, third.getValue().getUniqueBySession());
                    assertEquals(0f, third.getValue().getConversionRate(), 0);
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When: You have a URL Parameter  Experiment and try to get the results
     * Should: Send the right query to CubeJS Server and responding the right Result
     */
    @Test
    public void urlParameterGoalResults() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset targetPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithUrlParameterGoal(experimentPage);

        final String experimentNoDefaultVariantName = getNotDefaultVariantName(experiment);
        final Variant experimentNoDefaultVariant = APILocator.getVariantAPI().get(experimentNoDefaultVariantName).orElseThrow();

        final List<Map<String, String>> cubeJsQueryData = list(
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.day.day", "2023-10-27T00:00:00.000",
                        "Events.day", "2023-10-27T00:00:00.000",
                        "Events.totalSessions", "2",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "50"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", "2023-10-27T00:00:00.000",
                        "Events.day", "2023-10-27T00:00:00.000",
                        "Events.totalSessions", "1",
                        "Events.targetVisitedAfterSuccesses", "0",
                        "Events.targetVisitedAfterConvertionRate", "0"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", "2023-10-28T00:00:00.000",
                        "Events.day", "2023-10-28T00:00:00.000",
                        "Events.totalSessions", "3",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "33"
                ),
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.day.day", "2023-10-29T00:00:00.000",
                        "Events.day", "2023-10-29T00:00:00.000",
                        "Events.totalSessions", "4",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "25"
                )
        );
        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  Map.of("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.totalSessions", "4",
                        "Events.targetVisitedAfterSuccesses", "2",
                        "Events.targetVisitedAfterConvertionRate", "50"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "3",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "33"
                )
        );

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();

            setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(7, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(experimentNoDefaultVariantName)) {
                    assertEquals(experimentNoDefaultVariant.description().get(), variantResult.getVariantDescription());

                    Assert.assertEquals(2, variantResult.getUniqueBySession().getCount());
                    Assert.assertEquals(50.0f, variantResult.getUniqueBySession().getConversionRate(), 0);
                    Assert.assertEquals(4, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    final Map<String, ResultResumeItem> details = variantResult.getDetails();
                    assertEquals(3, details.size());

                    final Iterator<Map.Entry<String, ResultResumeItem>> iterator = details.entrySet().iterator();

                    final Map.Entry<String, ResultResumeItem> first = iterator.next();
                    assertEquals("2023-10-27", first.getKey());

                    assertEquals(2, first.getValue().getTotalSessions());
                    assertEquals(1, first.getValue().getUniqueBySession());
                    assertEquals(50f, first.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> second = iterator.next();
                    assertEquals("2023-10-28", second.getKey());

                    assertEquals(0, second.getValue().getTotalSessions());
                    assertEquals(0, second.getValue().getUniqueBySession());
                    assertEquals(0f, second.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> third = iterator.next();
                    assertEquals("2023-10-29", third.getKey());

                    assertEquals(4, third.getValue().getTotalSessions());
                    assertEquals(1, third.getValue().getUniqueBySession());
                    assertEquals(25f, third.getValue().getConversionRate(), 0);
                } else {
                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());

                    Assert.assertEquals(1, variantResult.getUniqueBySession().getCount());
                    Assert.assertEquals(33.0f, variantResult.getUniqueBySession().getConversionRate(), 0);
                    Assert.assertEquals(3, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    final Map<String, ResultResumeItem> details = variantResult.getDetails();
                    assertEquals(3, details.size());


                    final Iterator<Map.Entry<String, ResultResumeItem>> iterator = details.entrySet().iterator();

                    final Map.Entry<String, ResultResumeItem> first = iterator.next();
                    assertEquals("2023-10-27", first.getKey());

                    assertEquals(1, first.getValue().getTotalSessions());
                    assertEquals(0, first.getValue().getUniqueBySession());
                    assertEquals(0f, first.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> second = iterator.next();
                    assertEquals("2023-10-28", second.getKey());

                    assertEquals(3, second.getValue().getTotalSessions());
                    assertEquals(1, second.getValue().getUniqueBySession());
                    assertEquals(33f, second.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> third = iterator.next();
                    assertEquals("2023-10-29", third.getKey());

                    assertEquals(0, third.getValue().getTotalSessions());
                    assertEquals(0, third.getValue().getUniqueBySession());
                    assertEquals(0f, third.getValue().getConversionRate(), 0);
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * Method to test: {@link ExperimentsAPI#getResults(Experiment, User)}
     * When: You have a Bounce Rate Experiment and try to get the results
     * Should: Send the right query to CubeJS Server and responding the right Result
     */
    @Test
    public void bounceRateGoalResults() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = createExperimentWithBounceRateGoalAndVariant(experimentPage);

        final String experimentNoDefaultVariantName = getNotDefaultVariantName(experiment);
        final Variant experimentNoDefaultVariant = APILocator.getVariantAPI().get(experimentNoDefaultVariantName).orElseThrow();

        final List<Map<String, String>> cubeJsQueryData = list(
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.day.day", "2023-10-27T00:00:00.000",
                        "Events.day", "2023-10-27T00:00:00.000",
                        "Events.totalSessions", "2",
                        "Events.bounceRateSuccesses", "1",
                        "Events.bounceRateConvertionRate", "50"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", "2023-10-27T00:00:00.000",
                        "Events.day", "2023-10-27T00:00:00.000",
                        "Events.totalSessions", "1",
                        "Events.bounceRateSuccesses", "0",
                        "Events.bounceRateConvertionRate", "0"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", "2023-10-28T00:00:00.000",
                        "Events.day", "2023-10-28T00:00:00.000",
                        "Events.totalSessions", "3",
                        "Events.bounceRateSuccesses", "1",
                        "Events.bounceRateConvertionRate", "33"
                ),
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.day.day", "2023-10-29T00:00:00.000",
                        "Events.day", "2023-10-29T00:00:00.000",
                        "Events.totalSessions", "4",
                        "Events.bounceRateSuccesses", "1",
                        "Events.bounceRateConvertionRate", "25"
                )
        );
        final Map<String, List<Map<String, String>>> cubeJsQueryResult =  Map.of("data", cubeJsQueryData);

        APILocator.getExperimentsAPI()
                .start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final String cubeJSQueryExpected = getExpectedBounceRateQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", experimentNoDefaultVariantName,
                        "Events.totalSessions", "4",
                        "Events.targetVisitedAfterSuccesses", "2",
                        "Events.targetVisitedAfterConvertionRate", "50"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "3",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "33"
                )
        );

        final String totalSessionsQuery = getExpectedBounceRateTotalSesionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();

            setAnalyticsHelper(mockAnalyticsHelper);

            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();

            final ExperimentResults experimentResults = experimentsAPIImpl.getResults(
                    experiment,
                    APILocator.systemUser());

            mockhttpServer.validate();

            assertEquals(7, experimentResults.getSessions().getTotal());

            for (VariantResults variantResult : experimentResults.getGoals().get("primary").getVariants().values()) {

                if (variantResult.getVariantName().equals(experimentNoDefaultVariantName)) {
                    assertEquals(experimentNoDefaultVariant.description().get(), variantResult.getVariantDescription());

                    Assert.assertEquals(2, variantResult.getUniqueBySession().getCount());
                    Assert.assertEquals(50.0f, variantResult.getUniqueBySession().getConversionRate(), 0);
                    Assert.assertEquals(4, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    final Map<String, ResultResumeItem> details = variantResult.getDetails();
                    assertEquals(3, details.size());

                    final Iterator<Map.Entry<String, ResultResumeItem>> iterator = details.entrySet().iterator();

                    final Map.Entry<String, ResultResumeItem> first = iterator.next();
                    assertEquals("2023-10-27", first.getKey());

                    assertEquals(2, first.getValue().getTotalSessions());
                    assertEquals(1, first.getValue().getUniqueBySession());
                    assertEquals(50f, first.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> second = iterator.next();
                    assertEquals("2023-10-28", second.getKey());

                    assertEquals(0, second.getValue().getTotalSessions());
                    assertEquals(0, second.getValue().getUniqueBySession());
                    assertEquals(0f, second.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> third = iterator.next();
                    assertEquals("2023-10-29", third.getKey());

                    assertEquals(4, third.getValue().getTotalSessions());
                    assertEquals(1, third.getValue().getUniqueBySession());
                    assertEquals(25f, third.getValue().getConversionRate(), 0);
                } else {
                    assertEquals("DEFAULT", variantResult.getVariantName());
                    assertEquals("Original", variantResult.getVariantDescription());

                    Assert.assertEquals(1, variantResult.getUniqueBySession().getCount());
                    Assert.assertEquals(33.0f, variantResult.getUniqueBySession().getConversionRate(), 0);
                    Assert.assertEquals(3, (long) experimentResults.getSessions().getVariants().get(variantResult.getVariantName()));

                    final Map<String, ResultResumeItem> details = variantResult.getDetails();
                    assertEquals(3, details.size());


                    final Iterator<Map.Entry<String, ResultResumeItem>> iterator = details.entrySet().iterator();

                    final Map.Entry<String, ResultResumeItem> first = iterator.next();
                    assertEquals("2023-10-27", first.getKey());

                    assertEquals(1, first.getValue().getTotalSessions());
                    assertEquals(0, first.getValue().getUniqueBySession());
                    assertEquals(0f, first.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> second = iterator.next();
                    assertEquals("2023-10-28", second.getKey());

                    assertEquals(3, second.getValue().getTotalSessions());
                    assertEquals(1, second.getValue().getUniqueBySession());
                    assertEquals(33f, second.getValue().getConversionRate(), 0);

                    final Map.Entry<String, ResultResumeItem> third = iterator.next();
                    assertEquals("2023-10-29", third.getKey());

                    assertEquals(0, third.getValue().getTotalSessions());
                    assertEquals(0, third.getValue().getUniqueBySession());
                    assertEquals(0f, third.getValue().getConversionRate(), 0);
                }
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());

            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private static String getNotDefaultVariantName(Experiment experiment) {
        return experiment.trafficProportion().variants().stream()
                .filter(experimentVariant -> !experimentVariant.id().equals("DEFAULT"))
                .map(experimentVariant -> experimentVariant.id())
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must have a not DEFAULT variant"));
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Try to update with a User with not permission to edit the Experiment's Page
     * Should: Throw a {@link DotSecurityException}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingExperimentWithNoPagePermission() throws DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();
        final Experiment experiment = createExperiment(experimentPage, metric,
                RandomString.make(15));

        final Experiment experimentUpdated = Experiment.builder().from(experiment)
                .description("Updating the Experiment")
                .build();

        try {
            APILocator.getExperimentsAPI().save(experimentUpdated, limitedUser);
            throw new AssertionError("Should throw a DotSecurityException");
        } catch (DotSecurityException e) {
            assertEquals("You don't have permission to save the Experiment.", e.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Try to update with a User with:
     *
     * - Read permission to the Experiment's Page
     * - Edit rights for Template-Layouts on the site
     *
     * Should: Throw a {@link DotSecurityException}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingExperimentWithReadPagePermission() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_EDIT);

        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();
        final Experiment experiment = createExperiment(experimentPage, metric,
                new String[]{RandomString.make(15)});

        final Experiment experimentUpdated = Experiment.builder().from(experiment)
                .description("Updating the Experiment")
                .build();

        try {
            APILocator.getExperimentsAPI().save(experimentUpdated, limitedUser);
            throw new AssertionError("Should throw a DotSecurityException");
        } catch (DotSecurityException e) {
            assertEquals("You don't have permission to save the Experiment.", e.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Try to update with a User with:
     *
     * - Edit permission to the Experiment's Page
     * - But not Edit rights for Template-Layouts on the site
     *
     * Should: Throw a {@link DotSecurityException}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingExperimentWithEditPagePermissionButNoTemplateLayoutPermission() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_EDIT);

        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();
        final Experiment experiment = createExperiment(experimentPage, metric,
                RandomString.make(15));

        final Experiment experimentUpdated = Experiment.builder().from(experiment)
                .description("Updating the Experiment")
                .build();

        try {
            APILocator.getExperimentsAPI().save(experimentUpdated, limitedUser);
            throw new AssertionError("Should throw a DotSecurityException");
        } catch (DotSecurityException e) {
            assertEquals("You don't have permission to save the Experiment.", e.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#save(Experiment, User)}
     * When: Try to update with a User with:
     *
     * - Edit permission to the Experiment's Page
     * - Edit rights for Template-Layouts on the site
     *
     * Should: Update the Experiment
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void savingExperimentWithNotAdminUser() throws DotDataException, DotSecurityException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_EDIT);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_EDIT);

        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();
        final Experiment experiment = createExperiment(experimentPage, metric,
                new String[]{RandomString.make(15)});

        final Experiment experimentUpdated = Experiment.builder().from(experiment)
                .description("Updating the Experiment")
                .build();

        final Experiment experimentSaved = APILocator.getExperimentsAPI().save(experimentUpdated, limitedUser);

        assertEquals("Updating the Experiment", experimentSaved.description().orElseThrow());
    }


    private static Experiment createExperimentWithUrlParameterGoal(
            final HTMLPageAsset experimentPage) {
        return createExperimentWithUrlParameterGoal(experimentPage, Operator.EQUALS,
                new QueryParameter("testName", "testValue"));
    }

    private static Experiment createExperimentWithUrlParameterGoal(
            final HTMLPageAsset experimentPage, final Operator operator, final QueryParameter value) {

        final Condition queryParameterCondition = Condition.builder()
                .parameter("queryParameter")
                .value(value)
                .operator(operator)
                .build();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.URL_PARAMETER)
                .addConditions(queryParameterCondition)
                .build();

        return createExperiment(experimentPage, metric);
    }

    private Experiment createExperimentWithBounceRateGoalAndVariant(final HTMLPageAsset experimentPage) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.BOUNCE_RATE)
                .build();

        return createExperiment(experimentPage, metric);
    }

    private Experiment createExperimentWithExitRateGoalAndVariant(final HTMLPageAsset experimentPage) {
        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.EXIT_RATE)
                .build();

        return createExperiment(experimentPage, metric);
    }

    private List<Map<String, String>> createPageViewEvents(final Instant firstEventTime,
            final Experiment experiment,
            final String variantName,
            final HTMLPageAsset... pages) {
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
            dataList.add(Map.of(
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

    private static String getExpectedBounceRateQuery(Experiment experiment) {
        try {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            final String cubeJSQueryExpected = "{" +
                    "\"measures\": [" +
                        "\"Events.totalSessions\"," +
                        "\"Events.bounceRateSuccesses\"," +
                        "\"Events.bounceRateConvertionRate\"" +
                    "]," +
                    "\"dimensions\": [" +
                        "\"Events.variant\"" +
                    "]," +
                    "\"filters\": [" +
                        "{\n" +
                            "\"member\": \"Events.experiment\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.getIdentifier() + "\"" +
                            "]" +
                        "}," +
                        "{" +
                            "\"member\": \"Events.runningId\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.runningIds().getCurrent().get().id() + "\"" +
                            "]" +
                        "}" +
                    "]," +
                    "\"timeDimensions\": [" +
                        "{" +
                            "\"dimension\": \"Events.day\"," +
                            "\"granularity\": \"day\"" +
                            "}" +
                    "]," +
                    "\"order\":{\"Events.day\":\"asc\"}" +
                    "}";

            return cubeJSQueryExpected;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }


    private static String getExpectedExitRateQuery(Experiment experiment) {
        try {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            final String cubeJSQueryExpected = "{" +
                    "\"measures\": [" +
                        "\"Events.totalSessions\"," +
                        "\"Events.exitRateSuccesses\"," +
                        "\"Events.exitRateConvertionRate\"" +
                    "]," +
                    "\"dimensions\": [" +
                        "\"Events.variant\"" +
                    "]," +
                    "\"filters\": [" +
                        "{\n" +
                            "\"member\": \"Events.experiment\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                 "\"" + experimentFromDB.getIdentifier() + "\"" +
                            "]" +
                        "}," +
                        "{" +
                            "\"member\": \"Events.runningId\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.runningIds().getCurrent().get().id() + "\"" +
                            "]" +
                        "}" +
                    "]," +
                    "\"timeDimensions\": [" +
                            "{" +
                                "\"dimension\": \"Events.day\"," +
                                "\"granularity\": \"day\"" +
                            "}" +
                        "]," +
                        "\"order\":{\"Events.day\":\"asc\"}" +
                    "}";

            return cubeJSQueryExpected;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExpectedBounceRateTotalSesionsQuery(Experiment experiment) {
        try {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            final String cubeJSQueryExpected = "{" +
                    "\"measures\": [" +
                        "\"Events.totalSessions\"," +
                        "\"Events.bounceRateSuccesses\"," +
                        "\"Events.bounceRateConvertionRate\"" +
                    "]," +
                    "\"dimensions\": [" +
                        "\"Events.variant\"" +
                    "]," +
                    "\"filters\": [" +
                        "{\n" +
                            "\"member\": \"Events.experiment\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.getIdentifier() + "\"" +
                            "]" +
                        "}," +
                        "{" +
                            "\"member\": \"Events.runningId\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.runningIds().getCurrent().get().id() + "\"" +
                            "]" +
                        "}" +
                    "]," +
                    "\"order\":{\"Events.day\":\"asc\"}" +
                    "}";

            return cubeJSQueryExpected;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExpectedPageReachQuery(Experiment experiment) {
        try {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            final String cubeJSQueryExpected = "{" +
                "\"measures\": [" +
                    "\"Events.totalSessions\"," +
                    "\"Events.targetVisitedAfterSuccesses\"," +
                    "\"Events.targetVisitedAfterConvertionRate\"" +
                "]," +
                "\"dimensions\": [" +
                    "\"Events.variant\"" +
                "]," +
                "\"filters\": [" +
                    "{\n" +
                        "\"member\": \"Events.experiment\"," +
                        "\"operator\": \"equals\"," +
                        "\"values\": [" +
                            "\"" + experimentFromDB.getIdentifier() + "\"" +
                        "]" +
                    "}," +
                    "{" +
                        "\"member\": \"Events.runningId\"," +
                        "\"operator\": \"equals\"," +
                        "\"values\": [" +
                            "\"" + experimentFromDB.runningIds().getCurrent().get().id() + "\"" +
                        "]" +
                    "}" +
                "]," +
                "\"timeDimensions\": [" +
                    "{" +
                        "\"dimension\": \"Events.day\"," +
                        "\"granularity\": \"day\"" +
                    "}" +
                "]," +
                "\"order\":{\"Events.day\":\"asc\"}" +
            "}";

            return cubeJSQueryExpected;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExpectedReachPageTotalSessionsQuery(Experiment experiment) {
        try {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            final String cubeJSQueryExpected = "{" +
                        "\"measures\": [" +
                            "\"Events.totalSessions\"," +
                            "\"Events.targetVisitedAfterSuccesses\"," +
                            "\"Events.targetVisitedAfterConvertionRate\"" +
                        "]," +
                        "\"dimensions\": [" +
                            "\"Events.variant\"" +
                        "]," +
                        "\"filters\": [" +
                            "{\n" +
                                "\"member\": \"Events.experiment\"," +
                                "\"operator\": \"equals\"," +
                                "\"values\": [" +
                                    "\"" + experimentFromDB.getIdentifier() + "\"" +
                                "]" +
                            "}," +
                            "{" +
                                "\"member\": \"Events.runningId\"," +
                                "\"operator\": \"equals\"," +
                                "\"values\": [" +
                                  "\"" + experimentFromDB.runningIds().getCurrent().get().id() + "\"" +
                                "]" +
                            "}" +
                        "]," +
                        "\"order\":{\"Events.day\":\"asc\"}" +
                    "}";

            return cubeJSQueryExpected;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExpectedExitRateTotalSessionsQuery(Experiment experiment) {
        try {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            final String cubeJSQueryExpected = "{" +
                    "\"measures\": [" +
                        "\"Events.totalSessions\"," +
                        "\"Events.exitRateSuccesses\"," +
                        "\"Events.exitRateConvertionRate\"" +
                    "]," +
                    "\"dimensions\": [" +
                        "\"Events.variant\"" +
                    "]," +
                    "\"filters\": [" +
                        "{\n" +
                            "\"member\": \"Events.experiment\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.getIdentifier() + "\"" +
                            "]" +
                        "}," +
                        "{" +
                            "\"member\": \"Events.runningId\"," +
                            "\"operator\": \"equals\"," +
                            "\"values\": [" +
                                "\"" + experimentFromDB.runningIds().getCurrent().get().id() + "\"" +
                            "]" +
                        "}" +
                    "]," +
                    "\"order\":{\"Events.day\":\"asc\"}" +
            "}";

            return cubeJSQueryExpected;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addContext(final MockHttpServer mockHttpServer,
                                   final String uri,
                                   final String expectedQuery,
                                   final String responseBody) {
        final MockHttpServerContext.Builder serverContextBuilder = new MockHttpServerContext.Builder()
                .uri(uri)
                .requestCondition(requestContext ->
                                String.format(
                                        "Request is not right, %nExpected: %s%nCurrent %s",
                                        expectedQuery,
                                        requestContext.getRequestParameter("query").orElse(StringPool.BLANK)),
                        requestContext -> isEquals(expectedQuery, requestContext))
                .responseStatus(HttpURLConnection.HTTP_OK)
                .mustBeCalled();

        if (responseBody != null) {
            serverContextBuilder.responseBody(responseBody);
        }

        mockHttpServer.addContext(serverContextBuilder.build());
    }

    private static void addContext(final MockHttpServer mockHttpServer,
                                   final String expectedQuery,
                                   final String responseBody) {
        addContext(mockHttpServer, "/cubejs-api/v1/load", expectedQuery, responseBody);
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
        final Host host = new SiteDataGen().nextPersisted();reachPageGoalResults();
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
                .type(MetricType.EXIT_RATE)
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

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final List<Map<String, String>> data = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "50",
                        "Events.targetVisitedAfterConvertionRate", "83.33"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "16",
                        "Events.targetVisitedAfterConvertionRate", "26.66"
                )
        );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult = Map.of("data", data);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "50",
                        "Events.targetVisitedAfterConvertionRate", "83.33"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "16",
                        "Events.targetVisitedAfterConvertionRate", "26.66"
                )
        );

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));


        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();
            setAnalyticsHelper(mockAnalyticsHelper);
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
     * - You create an {@link Experiment} using the A page with a Exit Rate Goal
     * - You have the follow page_view Event in Different Sessions:
     *
     * Session 1 : A, this is a Exit Rate.
     * Session 2: A, B, This is not a Exit Rate.
     *
     * Should: calculate the probability that B beats A is 0.99
     */
    @Test
    public void test_calcBayesian_BOverA_ExitRate() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset pageB = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.EXIT_RATE)
                .addConditions(getUrlCondition(pageA.getPageUrl()))
                .build();

        final Experiment experiment = createExperiment(pageA, metric, RandomString.make(15));
        final String variantName = getNotDefaultVariantName(experiment);


        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final List<Map<String, String>> data = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "2",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "50"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "1",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "100"
                )
        );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult = Map.of("data", data);
        final String cubeJSQueryExpected = getExpectedExitRateQuery(experiment);

        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.totalSessions", "2",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "50.0"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "1",
                        "Events.targetVisitedAfterSuccesses", "1",
                        "Events.targetVisitedAfterConvertionRate", "100"
                )
        );

        final String totalSessionsQuery = getExpectedExitRateTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();
            setAnalyticsHelper(mockAnalyticsHelper);
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

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final List<Map<String, String>> data = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "35",
                        "Events.targetVisitedAfterConvertionRate", "58.33"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "45",
                        "Events.targetVisitedAfterConvertionRate", "75"
                )
        );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult = Map.of("data", data);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "35",
                        "Events.targetVisitedAfterConvertionRate", "0.0"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "45",
                        "Events.targetVisitedAfterConvertionRate", "75"
                )
        );

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();
            setAnalyticsHelper(mockAnalyticsHelper);
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


        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final List<Map<String, String>> data = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "35",
                        "Events.targetVisitedAfterConvertionRate", "58.33"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "45",
                        "Events.targetVisitedAfterConvertionRate", "75"
                )
        );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult = Map.of("data", data);
        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", variantName,
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "35",
                        "Events.targetVisitedAfterConvertionRate", "0.0"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "45",
                        "Events.targetVisitedAfterConvertionRate", "75"
                )
        );

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        IPUtils.disabledIpPrivateSubnet(true);

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();
            setAnalyticsHelper(mockAnalyticsHelper);

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


        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        IPUtils.disabledIpPrivateSubnet(true);

        final MockHttpServer mockhttpServer = new MockHttpServer(MOCK_SERVER_IP, MOCK_SERVER_PORT);

        final List<Map<String, String>> data = list(
                Map.of(
                        "Events.variant", variantBName,
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "50",
                        "Events.targetVisitedAfterConvertionRate", "83.33"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "16",
                        "Events.targetVisitedAfterConvertionRate", "26.66"
                ),
                Map.of(
                        "Events.variant", variantCName,
                        "Events.day.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.day", EVENTS_FORMATTER.format(Instant.now()),
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "55",
                        "Events.targetVisitedAfterConvertionRate", "91.66"
                )
        );

        final Map<String, List<Map<String, String>>> cubeJsQueryResult = Map.of("data", data);

        final String cubeJSQueryExpected = getExpectedPageReachQuery(experiment);
        addContext(mockhttpServer, cubeJSQueryExpected, JsonUtil.getJsonStringFromObject(cubeJsQueryResult));

        final List<Map<String, String>> totalSessionsQueryData = list(
                Map.of(
                        "Events.variant", variantBName,
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "16",
                        "Events.targetVisitedAfterConvertionRate", "83.33"
                ),
                Map.of(
                        "Events.variant", "DEFAULT",
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "16",
                        "Events.targetVisitedAfterConvertionRate", "26.66"
                ),
                Map.of(
                        "Events.variant", variantCName,
                        "Events.totalSessions", "60",
                        "Events.targetVisitedAfterSuccesses", "55",
                        "Events.targetVisitedAfterConvertionRate", "91.66"
                )
        );

        final String totalSessionsQuery = getExpectedReachPageTotalSessionsQuery(experiment);
        addContext(mockhttpServer, totalSessionsQuery, JsonUtil.getJsonStringFromObject(Map.of("data", totalSessionsQueryData)));

        mockhttpServer.start();

        try {
            final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
            setAnalyticsHelper(mockAnalyticsHelper);
            final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();
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
        final AnalyticsHelper mockAnalyticsHelper = AnalyticsTestUtils.mockInvalidAnalyticsHelper();
        setAnalyticsHelper(mockAnalyticsHelper);
        final ExperimentsAPIImpl experimentsAPIImpl = new ExperimentsAPIImpl();
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

    private static String getPageUri(HTMLPageAsset page) {
        try {
            return page.getURI();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#promoteVariant(String, String, User)}
     * When: A User without permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to promote a variant
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToPromoteVariantWithNoPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_PUBLISH);

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        final String variantName = getNotDefaultVariantName(experiment);

        try {
            APILocator.getExperimentsAPI()
                    .promoteVariant(experiment.id().get(), variantName, limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to promote a Variant. Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals("You don't have permission to get the Experiment. Experiment Id: " + experiment.getIdentifier(),
                    cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#promoteVariant(String, String, User)}
     * When: A User with READ permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to promote a variant
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToPromoteVariantWithReadPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        final String variantName = getNotDefaultVariantName(experiment);

        try {
            APILocator.getExperimentsAPI()
                    .promoteVariant(experiment.id().get(), variantName, limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to promote a Variant. Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals("You don't have permission to get the Experiment. Experiment Id: " + experiment.getIdentifier(),
                    cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#promoteVariant(String, String, User)}
     * When: A User with EDIT permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to promote a variant
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToPromoteVariantWithEditPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        final String variantName = getNotDefaultVariantName(experiment);

        try {
            APILocator.getExperimentsAPI()
                    .promoteVariant(experiment.id().get(), variantName, limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to promote a Variant. Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();
            final String causeExpectedMessage = String.format(
                    "User %s doesn't have permission to publish the Experiment's page. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().orElseThrow());

            assertEquals(causeExpectedMessage, cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#promoteVariant(String, String, User)}
     * When: A User with PUBLISH permission on the Experiment's Page but no Publish rights for Template-Layouts on the site
     * try to promote a variant
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToPromoteVariantWithPublishPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_EDIT, PermissionAPI.PERMISSION_PUBLISH);

        final String variantName = getNotDefaultVariantName(experiment);

        try {
            APILocator.getExperimentsAPI()
                    .promoteVariant(experiment.id().get(), variantName, limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to promote a Variant. Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();
            final String causeExpectedMessage = String.format(
                    "User %s doesn't have PUBLISH permission for Template-Layouts on the Experiment Page's site. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().orElseThrow());

            assertEquals(causeExpectedMessage, cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(String, String, User)}
     * When: A User without permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to start the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToStartExperimentWithNoPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_PUBLISH);

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();
        try {
            APILocator.getExperimentsAPI().start(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to start the Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals("You don't have permission to get the Experiment. Experiment Id: " + experiment.getIdentifier(),
                    cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(String, User)}
     * When: A User with READ permission on the page and Publish rights for Template-Layouts on the site
     * try to start the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToStartExperimentWithReadPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        try {
            APILocator.getExperimentsAPI().start(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to start the Experiment Id: "
                    + experiment.id().get();

            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals("You don't have permission to get the Experiment. Experiment Id: " + experiment.getIdentifier(),
                    cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(String, User)}
     * When: A User with EDIT permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to start the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToStartExperimentWithEditPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        try {
            APILocator.getExperimentsAPI().start(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to start the Experiment Id: "
                    + experiment.id().get();

            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();
            final String causeExpectedMessage = String.format(
                    "User %s doesn't have PUBLISH permission on the Experiment Page's. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().orElseThrow());

            assertEquals(causeExpectedMessage, cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(String, User)}
     * When: A User with PUBLISH permission on the Experiment's Page and not Publish rights for Template-Layouts on the site
     * try to start the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToStartExperimentWithNotTemplateLayoutsPermissions() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT, PermissionAPI.PERMISSION_PUBLISH);

        try {
            APILocator.getExperimentsAPI().start(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to start the Experiment Id: "
                    + experiment.id().get();

            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();
            final String causeExpectedMessage = String.format(
                    "User %s doesn't have PUBLISH permission for Template-Layouts on the Experiment Page's site. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().orElseThrow());

            assertEquals(causeExpectedMessage, cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#end(String, User)}
     * When: A User without permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to end the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToEndExperimentWithNoPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_PUBLISH);

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();
        try {
            APILocator.getExperimentsAPI().end(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to end the Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals("You don't have permission to get the Experiment. Experiment Id: " + experiment.id().get(),
                    cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#end(String, User)}
     * When: A User with READ permission on the page and Publish rights for Template-Layouts on the site
     * try to end the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToEndExperimentWithReadPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        try {
            APILocator.getExperimentsAPI().end(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to end the Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals("You don't have permission to get the Experiment. Experiment Id: " + experiment.id().get(),
                    cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#end(String, String, User)}
     * When: A User with EDIT permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to end the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToEndExperimentWithEditPermissionOnThePage() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        try {
            APILocator.getExperimentsAPI().end(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to end the Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            assertEquals(String.format("User %s doesn't have permission to publish the Experiment's page. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().get()), cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#end(String, String, User)}
     * When: A User with PUBLISH permission on the Experiment's Page and not Publish rights for Template-Layouts on the site
     * try to end the Experiment
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToEndExperimentWithNotTemplateLayoutsPermissions() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT, PermissionAPI.PERMISSION_PUBLISH);

        try {
            APILocator.getExperimentsAPI().end(experiment.id().get(), limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to end the Experiment Id: "
                    + experiment.id().get();

            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();
            final String causeExpectedMessage = String.format(
                    "User %s doesn't have PUBLISH permission for Template-Layouts on the Experiment Page's site. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().orElseThrow());

            assertEquals(causeExpectedMessage, cause.getMessage());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#end(String, String, User)}
     * When: A Not Admin User with PUBLISH permission on the Experiment's Page and Publish rights for Template-Layouts on the site
     * try to end the Experiment
     * Should: End the Experiment
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToEndExperimentWithNotAdminUser() throws DotDataException, DotSecurityException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT, PermissionAPI.PERMISSION_PUBLISH);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        APILocator.getExperimentsAPI().start(experiment.id().get(), limitedUser);

        APILocator.getExperimentsAPI().end(experiment.id().get(), limitedUser);

        final Optional<Experiment> experimentFromdataBase = APILocator.getExperimentsAPI()
                .find(experiment.id().get(), APILocator.systemUser());

        assertEquals(Experiment.Status.ENDED, experimentFromdataBase.get().status());
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#promoteVariant(String, String, User)}
     * When: A User with EDIT permission on the page try to promote a variant
     * Should: thrown a {@link DotSecurityException}
     *
     * @throws DotDataException
     */
    @Test()
    public void tryToPromoteVariantWithEditPermissionOnThePageAndTemplateLayoutPermission() throws DotDataException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .addVariant("variantA")
                .page(experimentPage)
                .nextPersisted();

        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ,
                PermissionAPI.PERMISSION_EDIT);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_PUBLISH);

        final String variantName = getNotDefaultVariantName(experiment);

        try {
            APILocator.getExperimentsAPI()
                    .promoteVariant(experiment.id().get(), variantName, limitedUser);

            throw new AssertionError("Should thrown a DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String messageExpected = "You don't have permission to promote a Variant. Experiment Id: "
                    + experiment.id().get();
            assertEquals(messageExpected, e.getMessage());

            final Throwable cause = e.getCause();

            final String errorMessageExpected = String.format(
                    "User %s doesn't have permission to publish the Experiment's page. Experiment Id: %s",
                    limitedUser.getUserId(), experiment.id().orElseThrow());

            assertEquals(errorMessageExpected, cause.getMessage());
        }
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - Crate an Experiment with two {@link Variant}.
     * - Create two {@link Contentlet} and create a version into the NO DEFAULT Experiment's  {@link Variant} also
     * create version to the DEFAULT Variant, Save and publish them.
     * - Make any change to the {@link Contentlet} and just save.
     *
     * With a Not Admin User with the follow permissions
     * - Publish rights on the Page
     * - Publish rights for Template-Layouts on the site
     *
     * Do:
     *
     * - Start the Experiment
     * - Promote the {@link Variant} with No Admin User
     *
     * Should:
     * - Copy both version of the specific Variant  and turn it into the WORKING/LIVE DEFAULT Variant
     */
    @Test
    public void promoteWithNotAdminUser() throws DotDataException, DotSecurityException {

        final Role role = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(role).nextPersisted();

        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2")
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .addVariant("Test Variant")
                .nextPersisted();

        final String variantName = getNotDefaultVariantName(experiment);

        final Variant variant = APILocator.getVariantAPI().get(variantName).orElseThrow();

        final Contentlet contentlet1Variant = ContentletDataGen.createNewVersion(contentlet1,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant"
                ));
        final Contentlet contentlet2Variant = ContentletDataGen.createNewVersion(contentlet2,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant"
                ));

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        APILocator.getContentletAPI().publish(contentlet1Variant, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant, APILocator.systemUser(), false);

        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {

            ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1"));
            ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2"));
            ContentletDataGen.update(contentlet1Variant,
                    Map.of("title", "WORKING contentlet1_variant"));
            ContentletDataGen.update(contentlet2Variant,
                    Map.of("title", "WORKING contentlet2_variant"));

           addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                    PermissionAPI.PERMISSION_READ,
                    PermissionAPI.PERMISSION_EDIT, PermissionAPI.PERMISSION_PUBLISH);
            addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                    PermissionAPI.PERMISSION_PUBLISH);

            APILocator.getExperimentsAPI()
                    .promoteVariant(experiment.id().orElseThrow(), variantName, limitedUser);


            checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT,
                    "WORKING contentlet1_variant",
                    titleField);
            checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant",
                    titleField);

            checkVersion(contentlet1, false, variant, "WORKING contentlet1_variant", titleField);
            checkVersion(contentlet1, true, variant, "LIVE contentlet1_variant", titleField);

            checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT,
                    "WORKING contentlet2_variant",
                    titleField);
            checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant",
                    titleField);

            checkVersion(contentlet2, true, variant, "LIVE contentlet2_variant", titleField);
            checkVersion(contentlet2, false, variant, "WORKING contentlet2_variant", titleField);
        } finally {

            final Experiment experimentFromDataBase = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            if (experimentFromDataBase.status() == Experiment.Status.RUNNING) {
                APILocator.getExperimentsAPI()
                        .end(experiment.id().orElseThrow(), APILocator.systemUser());
            }
        }
    }

    private static void addPermission(final Permissionable permissionable,
            final User limitedUser, final String permissionType, final int... permissions) throws DotDataException {

        final int permission = Arrays.stream(permissions).sum();

        final Permission permissionObject = new Permission(permissionType,
                permissionable.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                permission, true);

        try {
            APILocator.getPermissionAPI().save(permissionObject, permissionable,
                    APILocator.systemUser(), false);
        } catch (DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#getRunningExperimentPerPage(String)}
     * When: The Page has a Experiment but is not RUNNING
     * Should: The Method return a empty Optional but after Start the Experiment the
     * method should return the Experiment
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getRunningExperimentPerPage() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .page(experimentPage)
                .nextPersisted();

        final Optional<Experiment> runningExperiment = APILocator.getExperimentsAPI()
                .getRunningExperimentPerPage(experimentPage.getIdentifier());

        assertFalse(runningExperiment.isPresent());

        APILocator.getExperimentsAPI().start(experiment.getIdentifier(), APILocator.systemUser());

        try {
            final Optional<Experiment> runningExperiment2 = APILocator.getExperimentsAPI()
                    .getRunningExperimentPerPage(experimentPage.getIdentifier());

            assertTrue(runningExperiment2.isPresent());
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
        }
    }
    private static void checkVersion(final Contentlet contentlet, final boolean live,
            final Variant defaultVariant, final String value, final Field titleField)
            throws DotDataException, DotSecurityException {

        checkVersion(contentlet, live, defaultVariant, APILocator.getLanguageAPI().getDefaultLanguage(),
                value, titleField);
    }
    private static void checkVersion(Contentlet contentlet, boolean live, Variant variant,
            final Language language, final String  value, Field titleField)
            throws DotDataException, DotSecurityException {

        final Contentlet contentlet1DefaultVariantFromDataBase = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(),
                        live, language.getId(), variant.name(), APILocator.systemUser(), false);

        Assert.assertEquals(value, contentlet1DefaultVariantFromDataBase
                .getStringProperty(titleField.variable()));
    }



    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When Try to archive a Page with a Running Experiment
     * Should: Throw a {@link DotDataException}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryArchivePageWithRunningExperiment() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        APILocator.getExperimentsAPI()
                .start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {
            APILocator.getContentletAPI()
                    .archive(experimentPage, APILocator.systemUser(), false);
            final Contentlet experimentPageAfterArchived = APILocator.getContentletAPI()
                    .find(experimentPage.getInode(), APILocator.systemUser(), false);
            assertTrue(experimentPageAfterArchived.isArchived());

            final Optional<Experiment> experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser());

            assertTrue(experimentFromDB.isPresent());

            assertEquals(Status.RUNNING, experimentFromDB.get().status());
        } finally {
            APILocator.getExperimentsAPI().end(experiment.id().orElseThrow(), APILocator.systemUser());
        }
    }

    /**
     * Method to test: Several method, we need to test that all the possible actio to an Experiment work
     * for an Orphan Experiment
     */
    @Test
    public void orphanExperiment() throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        APILocator.getExperimentsAPI().save(experiment, APILocator.systemUser());
        Optional<Experiment> experimentFromDB = APILocator.getExperimentsAPI()
                .find(experiment.id().orElseThrow(), APILocator.systemUser());

        assertTrue(experimentFromDB.isPresent());
        assertEquals(experiment.description(), experimentFromDB.get().description());

        final Experiment experimentWithDescription = experiment.withDescription("New Description");

        APILocator.getExperimentsAPI().save(experimentWithDescription, APILocator.systemUser());

        experimentFromDB = APILocator.getExperimentsAPI()
                .find(experiment.id().orElseThrow(), APILocator.systemUser());

        assertTrue(experimentFromDB.isPresent());
        assertEquals("New Description", experimentFromDB.get().description().get());

        try {
            APILocator.getExperimentsAPI()
                    .start(experiment.id().orElseThrow(), APILocator.systemUser());

            experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser());

            assertTrue(experimentFromDB.isPresent());
            assertEquals(Status.RUNNING, experimentFromDB.get().status());

            APILocator.getExperimentsAPI()
                    .cancel(experiment.id().orElseThrow(), APILocator.systemUser());
        } catch (Exception e) {
            APILocator.getExperimentsAPI().end(experiment.id().orElseThrow(), APILocator.systemUser());
            throw e;
        }

        experimentFromDB = APILocator.getExperimentsAPI()
                .find(experiment.id().orElseThrow(), APILocator.systemUser());

        assertTrue(experimentFromDB.isPresent());
        assertEquals(Status.DRAFT, experimentFromDB.get().status());

        final Experiment experiment_2 = new ExperimentDataGen().nextPersisted();

        APILocator.getExperimentsAPI()
                .start(experiment_2.id().orElseThrow(), APILocator.systemUser());

        APILocator.getExperimentsAPI().end(experiment_2.id().get(), APILocator.systemUser());

        Optional<Experiment> experimentFromDB_2 = APILocator.getExperimentsAPI()
                .find(experiment_2.id().orElseThrow(), APILocator.systemUser());

        assertTrue(experimentFromDB_2.isPresent());
        assertEquals(Status.ENDED, experimentFromDB_2.get().status());

        APILocator.getExperimentsAPI().archive(experiment_2.id().get(), APILocator.systemUser());

        experimentFromDB = APILocator.getExperimentsAPI()
                .find(experiment_2.id().orElseThrow(), APILocator.systemUser());

        assertTrue(experimentFromDB_2.isPresent());
        assertEquals(Status.ENDED, experimentFromDB_2.get().status());

        final Experiment experiment_3 = new ExperimentDataGen().nextPersisted();


        APILocator.getExperimentsAPI().delete(experiment_3.id().get(), APILocator.systemUser());

        Optional<Experiment> experimentFromDB_3 = APILocator.getExperimentsAPI()
                .find(experiment_3.id().orElseThrow(), APILocator.systemUser());

        assertFalse(experimentFromDB_3.isPresent());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When Try to archive a Page with a DRAFT Experiment
     * Should: Archive the page and keep the Experiment on DRAFT
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryArchivePageWithNotRunningExperiment() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        APILocator.getContentletAPI()
                .archive(experimentPage, APILocator.systemUser(), false);

        final Contentlet contentlet = APILocator.getContentletAPI()
                .find(experimentPage.getInode(), APILocator.systemUser(), false);

        assertTrue(contentlet.isArchived());

        final Experiment experimentFromDatabase = APILocator.getExperimentsAPI()
                .find(experiment.getIdentifier(), APILocator.systemUser()).orElseThrow();

        assertEquals(Experiment.Status.DRAFT, experimentFromDatabase.status());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When Try to delete an Experiment
     * Should: Delete the Experiment and the Variants
     * Also must delete the Variants and the COntentlets' version inside the Variants
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryDeleteExperiment() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment draftExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String notDefaultVariantName = getNotDefaultVariantName(draftExperiment);
        final Variant notDefaultVariantStartedExperiment = APILocator.getVariantAPI()
                .get(notDefaultVariantName)
                .orElseThrow();

        final Contentlet contentlet_1 = createContentletWithWorkingAndLiveVersion(notDefaultVariantStartedExperiment);

        APILocator.getExperimentsAPI().delete(draftExperiment.id().orElseThrow(), APILocator.systemUser());

        final Optional<Experiment> draftExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(draftExperiment.getIdentifier(), APILocator.systemUser());

        assertFalse(draftExperimentFromDatabase.isPresent());

        final Optional<Variant> notDefaultVariantStartedExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantName);

        assertFalse(notDefaultVariantStartedExperimentOptional.isPresent());

        checkNotExistsAnyVersion(contentlet_1);

    }

    private static void checkNotExistsAnyVersion(Contentlet contentlet_1)
            throws DotDataException, DotSecurityException {
        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentlet_1.getIdentifier());

        final List<Contentlet> allVersionsAfterDeleted = APILocator.getContentletAPI()
                .findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(),
                        false);

        Assert.assertTrue(allVersionsAfterDeleted.isEmpty());
    }

    private static Contentlet createContentletWithWorkingAndLiveVersion(
            final Variant notDefaultVariantStartedExperiment)
            throws DotDataException, DotSecurityException {

        final Field textField = new FieldDataGen()
                .name("text")
                .velocityVarName("text")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(textField)
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(textField.variable(), "LIVE")
                .variant(notDefaultVariantStartedExperiment)
                .nextPersisted();

        final Contentlet variantLive = ContentletDataGen.createNewVersion(contentlet_1,
                notDefaultVariantStartedExperiment,
                Map.of(textField.variable(), "Variant LIVE"));

        ContentletDataGen.publish(variantLive);
        ContentletDataGen.update(variantLive, Map.of(textField.variable(), "WORKING"));
        return contentlet_1;
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When Try to delete a Page with two Experiments: one Draft and the other Ended
     * Should: Delete the Page and the experiments too
     * Also must delete the Variants and the COntentlets' version inside the Variants
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryDeletePageWithNotRunningExperiment() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment draftExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();
        final Experiment endedExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();
        final Experiment runningExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String notDefaultVariantDraftExperimentName = getNotDefaultVariantName(draftExperiment);
        final Variant notDefaultVariantDraftExperiment = APILocator.getVariantAPI()
                .get(notDefaultVariantDraftExperimentName)
                .orElseThrow();

        final Contentlet contentlet_1 = createContentletWithWorkingAndLiveVersion(notDefaultVariantDraftExperiment);

        final String notDefaultVariantEndedExperimentName = getNotDefaultVariantName(endedExperiment);
        final Variant notDefaultVariantEndedExperiment = APILocator.getVariantAPI()
                .get(notDefaultVariantEndedExperimentName)
                .orElseThrow();
        final Contentlet contentlet_2 = createContentletWithWorkingAndLiveVersion(notDefaultVariantEndedExperiment);

        APILocator.getExperimentsAPI().start(endedExperiment.id().orElseThrow(), APILocator.systemUser());
        APILocator.getExperimentsAPI().end(endedExperiment.id().orElseThrow(), APILocator.systemUser());

        APILocator.getExperimentsAPI().start(runningExperiment.id().orElseThrow(), APILocator.systemUser());

        APILocator.getContentletAPI()
                .archive(experimentPage, APILocator.systemUser(), false);

        APILocator.getContentletAPI()
                .delete(experimentPage, APILocator.systemUser(), false);

        final Contentlet contentlet = APILocator.getContentletAPI()
                .find(experimentPage.getInode(), APILocator.systemUser(), false);

        assertNull(contentlet);

        final Optional<Experiment> draftExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(draftExperiment.getIdentifier(), APILocator.systemUser());

        assertFalse(draftExperimentFromDatabase.isPresent());

        final Optional<Experiment> endedExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(endedExperiment.getIdentifier(), APILocator.systemUser());

        assertFalse(endedExperimentFromDatabase.isPresent());

        final Optional<Variant> notDefaultVariantDraftExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantDraftExperimentName);

        assertFalse(notDefaultVariantDraftExperimentOptional.isPresent());

        final Optional<Variant> notDefaultVariantEndedExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantEndedExperimentName);

        assertFalse(notDefaultVariantEndedExperimentOptional.isPresent());

        checkNotExistsAnyVersion(contentlet_1);
        checkNotExistsAnyVersion(contentlet_2);

        final Optional<Experiment> runningExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(runningExperiment.getIdentifier(), APILocator.systemUser());

        assertFalse(runningExperimentFromDatabase.isPresent());
    }


    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When A Not Admin user but with the follow permission:
     * - Not Edit Permission on the Page
     * - Edit Template/Layout Permission on the site.
     *
     * Try to delete a Page with two Experiments: one Draft and the other Ended
     * Should: throw DotSecurityException
     * Also must delete the Variants and the COntentlets' version inside the Variants
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryDeletePageWithNotRunningExperimentNoAdminUserNotEditPagePermission() throws DotDataException, DotSecurityException {
        final User limitedUser = new UserDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(), PermissionAPI.PERMISSION_EDIT);

        final Experiment draftExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();
        final Experiment endedExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String notDefaultVariantDraftExperimentName = getNotDefaultVariantName(draftExperiment);
        final String notDefaultVariantEndedExperimentName = getNotDefaultVariantName(endedExperiment);

        APILocator.getExperimentsAPI().start(endedExperiment.id().orElseThrow(), APILocator.systemUser());
        APILocator.getExperimentsAPI().end(endedExperiment.id().orElseThrow(), APILocator.systemUser());

        APILocator.getContentletAPI()
                .archive(experimentPage, APILocator.systemUser(), false);

        try {
            APILocator.getContentletAPI()
                    .delete(experimentPage, limitedUser, false);

            throw new AssertionError("Should throw DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String expectedMessage =  String.format("User: %s does not have Edit Permissions to lock content: %s",
                    limitedUser.getUserId(), experimentPage.getIdentifier());
            assertEquals(expectedMessage, e.getMessage());
        }

        final Contentlet contentlet = APILocator.getContentletAPI()
                .find(experimentPage.getInode(), APILocator.systemUser(), false);

        assertNotNull(contentlet);

        final Optional<Experiment> draftExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(draftExperiment.getIdentifier(), APILocator.systemUser());

        assertTrue(draftExperimentFromDatabase.isPresent());

        final Optional<Experiment> endedExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(endedExperiment.getIdentifier(), APILocator.systemUser());

        assertTrue(endedExperimentFromDatabase.isPresent());

        final Optional<Variant> notDefaultVariantDraftExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantDraftExperimentName);

        assertTrue(notDefaultVariantDraftExperimentOptional.isPresent());

        final Optional<Variant> notDefaultVariantEndedExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantEndedExperimentName);

        assertTrue(notDefaultVariantEndedExperimentOptional.isPresent());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When A Not Admin user but with the follow permission:
     * - Edit Permission on the Page
     * - Not Edit Template/Layout Permission on the site.
     *
     * Try to delete a Page with two Experiments: one Draft and the other Ended
     * Should: Delete the Page and the experiments too
     * Also must delete the Variants and the COntentlets' version inside the Variants
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryDeletePageWithNotRunningExperimentNoAdminUserNotTemplateLayoutPermission() throws DotDataException, DotSecurityException {

        final User limitedUser = new UserDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);

        final Experiment draftExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();
        final Experiment endedExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String notDefaultVariantDraftExperimentName = getNotDefaultVariantName(draftExperiment);
        final String notDefaultVariantEndedExperimentName = getNotDefaultVariantName(endedExperiment);

        APILocator.getExperimentsAPI().start(endedExperiment.id().orElseThrow(), APILocator.systemUser());
        APILocator.getExperimentsAPI().end(endedExperiment.id().orElseThrow(), APILocator.systemUser());

        APILocator.getContentletAPI()
                .archive(experimentPage, APILocator.systemUser(), false);

        try {
            APILocator.getContentletAPI()
                    .delete(experimentPage, limitedUser, false);

            throw new AssertionError("Should throw DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String expectedMessage =  String.format("User: %s does not have Edit Permissions to lock content: %s",
                    limitedUser.getUserId(), experimentPage.getIdentifier());
            assertEquals(expectedMessage, e.getMessage());
        }

        final Contentlet contentlet = APILocator.getContentletAPI()
                .find(experimentPage.getInode(), APILocator.systemUser(), false);

        assertNotNull(contentlet);

        final Optional<Experiment> draftExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(draftExperiment.getIdentifier(), APILocator.systemUser());

        assertTrue(draftExperimentFromDatabase.isPresent());

        final Optional<Experiment> endedExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(endedExperiment.getIdentifier(), APILocator.systemUser());

        assertTrue(endedExperimentFromDatabase.isPresent());

        final Optional<Variant> notDefaultVariantDraftExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantDraftExperimentName);

        assertTrue(notDefaultVariantDraftExperimentOptional.isPresent());

        final Optional<Variant> notDefaultVariantEndedExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantEndedExperimentName);

        assertTrue(notDefaultVariantEndedExperimentOptional.isPresent());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#delete(Contentlet, User, boolean)}
     * When A Not Admin user but with the follow permission:
     * - Edit Permission on the Page
     * - Edit Template/Layout Permission on the site.
     *
     * Try to delete a Page with two Experiments: one Draft and the other Ended
     * Should: Delete the Page and the experiments too
     * Also must delete the Variants and the COntentlets' version inside the Variants
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void tryDeletePageWithNotRunningExperimentNoAdminUser() throws DotDataException, DotSecurityException {
        final User limitedUser = new UserDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment draftExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();
        final Experiment endedExperiment = new ExperimentDataGen().page(experimentPage).nextPersisted();

        final String notDefaultVariantDraftExperimentName = getNotDefaultVariantName(draftExperiment);
        final String notDefaultVariantEndedExperimentName = getNotDefaultVariantName(endedExperiment);

        APILocator.getExperimentsAPI().start(endedExperiment.id().orElseThrow(), APILocator.systemUser());
        APILocator.getExperimentsAPI().end(endedExperiment.id().orElseThrow(), APILocator.systemUser());

        APILocator.getContentletAPI()
                .archive(experimentPage, APILocator.systemUser(), false);

        try {
            APILocator.getContentletAPI()
                    .delete(experimentPage, limitedUser, false);

            throw new AssertionError("Should throw DotSecurityException");
        } catch (DotSecurityException e) {
            //expected
            final String expectedMessage =  String.format("User: %s does not have Edit Permissions to lock content: %s",
                    limitedUser.getUserId(), experimentPage.getIdentifier());
            assertEquals(expectedMessage, e.getMessage());
        }

        final Contentlet contentlet = APILocator.getContentletAPI()
                .find(experimentPage.getInode(), APILocator.systemUser(), false);

        assertNotNull(contentlet);

        final Optional<Experiment> draftExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(draftExperiment.getIdentifier(), APILocator.systemUser());

        assertTrue(draftExperimentFromDatabase.isPresent());

        final Optional<Experiment> endedExperimentFromDatabase = APILocator.getExperimentsAPI()
                .find(endedExperiment.getIdentifier(), APILocator.systemUser());

        assertTrue(endedExperimentFromDatabase.isPresent());

        final Optional<Variant> notDefaultVariantDraftExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantDraftExperimentName);

        assertTrue(notDefaultVariantDraftExperimentOptional.isPresent());

        final Optional<Variant> notDefaultVariantEndedExperimentOptional = APILocator.getVariantAPI()
                .get(notDefaultVariantEndedExperimentName);

        assertTrue(notDefaultVariantEndedExperimentOptional.isPresent());
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#cancel(String, User)}
     * When: Try to cancel a Scheduled Experiment
     * Should: The Experiment come back to DRAFT
     */
    @Test
    public void cancelScheduledExperiment() throws DotDataException, DotSecurityException {
        final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
        final Experiment experiment =  new ExperimentDataGen()
                .scheduling(Scheduling.builder().startDate(startDate).build())
                .nextPersisted();

        APILocator.getExperimentsAPI().start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {
            final Experiment experimentAfterScheduled = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.SCHEDULED, experimentAfterScheduled.status());

            APILocator.getExperimentsAPI().cancel(experimentAfterScheduled.getIdentifier(), APILocator.systemUser());

            final Experiment experimentAfterCancel = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.DRAFT, experimentAfterCancel.status());
        } finally {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            if (experimentFromDB.status() == Status.RUNNING) {
                APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            }
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#cancel(String, User)}
     * When: Try to cancel a Running Experiment
     * Should: The Experiment come back to DRAFT
     * Also the Running Experiment Cache must be empty after cancel an Experiment
     */
    @Test
    public void cancelRunningExperiment() throws DotDataException, DotSecurityException {
        final Experiment experiment =  new ExperimentDataGen()
                .nextPersisted();

        APILocator.getExperimentsAPI().start(experiment.id().orElseThrow(), APILocator.systemUser());

        APILocator.getExperimentsAPI().getRunningExperiments();
        assertCachedRunningExperiments(list(experiment.id().get()));

        try {
            final Experiment experimentAfterRunning = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.RUNNING, experimentAfterRunning.status());

            APILocator.getExperimentsAPI()
                    .cancel(experimentAfterRunning.getIdentifier(), APILocator.systemUser());

            assertCachedRunningExperiments(Collections.emptyList());

            final Experiment experimentAfterCancel = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.DRAFT, experimentAfterCancel.status());

            final Scheduling scheduling = experimentAfterCancel.scheduling().orElseThrow();

            assertFalse(scheduling.endDate().isPresent());
            assertFalse(scheduling.startDate().isPresent());
        } finally {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            if (experimentFromDB.status() == Status.RUNNING) {
                APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            }
        }
    }


    /**
     * Method to test: {@link ExperimentsAPIImpl#cancel(String, User)}
     * When: Try to cancel a Running Experiment with a no Admin User but with:
     * - Edit rights for the Page
     * - Edit rights for Template-Layouts on the site
     * Should: The Experiment come back to DRAFT
     */
    @Test
    public void cancelExperimentWithNoAdminUser() throws DotDataException, DotSecurityException {
        final User limitedUser = new UserDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();


        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_EDIT);

        final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
        final Experiment experiment =  new ExperimentDataGen()
                .page(experimentPage)
                .nextPersisted();

        APILocator.getExperimentsAPI().start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {
            final Experiment experimentAfterScheduled = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.RUNNING, experimentAfterScheduled.status());

            APILocator.getExperimentsAPI().cancel(experimentAfterScheduled.getIdentifier(), limitedUser);

            final Experiment experimentAfterCancel = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.DRAFT, experimentAfterCancel.status());
        } finally {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            if (experimentFromDB.status() == Status.RUNNING) {
                APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            }
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#cancel(String, User)}
     * When: Try to cancel a Running Experiment with a no Admin User:
     * - Without Edit rights for the Page
     * - With Edit rights for Template-Layouts on the site
     * Should: Throw DotSecurityException
     */
    @Test
    public void cancelExperimentWithUserNotEditPagePermission() throws DotDataException, DotSecurityException {
        final User limitedUser = new UserDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();


        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ);
        addPermission(host, limitedUser, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_EDIT);

        final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
        final Experiment experiment =  new ExperimentDataGen()
                .page(experimentPage)
                .nextPersisted();

        APILocator.getExperimentsAPI().start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {
            final Experiment experimentAfterScheduled = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.RUNNING, experimentAfterScheduled.status());

            try {
                APILocator.getExperimentsAPI()
                        .cancel(experimentAfterScheduled.getIdentifier(), limitedUser);
                throw new AssertionError("Should throw DotSecurityException");
            } catch (DotSecurityException e) {
                //expected
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#cancel(String, User)}
     * When: Try to cancel a Running Experiment with a no Admin User:
     * - With Edit rights for the Page
     * - Without Edit rights for Template-Layouts on the site
     * Should: Throw DotSecurityException
     */
    @Test
    public void cancelExperimentWithUserNotEditTemplateLayoutPermission() throws DotDataException, DotSecurityException {
        final User limitedUser = new UserDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();


        addPermission(experimentPage, limitedUser, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_EDIT);

        final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
        final Experiment experiment =  new ExperimentDataGen()
                .page(experimentPage)
                .nextPersisted();

        APILocator.getExperimentsAPI().start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {
            final Experiment experimentAfterScheduled = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertEquals(Status.RUNNING, experimentAfterScheduled.status());

            try {
                APILocator.getExperimentsAPI()
                        .cancel(experimentAfterScheduled.getIdentifier(), limitedUser);
                throw new AssertionError("Should throw DotSecurityException");
            } catch (DotSecurityException e) {
                //expected
            }
        } finally {
            APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
        }
    }

    private void setAnalyticsHelper(final AnalyticsHelper mockAnalyticsHelper) {
        CubeJSClientFactoryImpl.setAnalyticsHelper(mockAnalyticsHelper);
    }


    /**
     * Method to test: {@link Experiment} H22 Serialization
     * When: Try to Serialize a {@link Experiment}
     * Should: not throw any exception
     *
     * @throws IOException
     */
    @Test
    public void testExperimentSerialization() throws IOException, ClassNotFoundException {

        final Experiment experiment = new ExperimentDataGen()
                .description("Experiment Description")
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        byte[] bytes = null;

        try(
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(os, 8192)); ){
            output.writeObject(experiment);
            output.flush();

            bytes = os.toByteArray();
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))){
            final Experiment experimentFromBytes = (Experiment) input.readObject();

            Assert.assertEquals(experiment.name(), experimentFromBytes.name());
            Assert.assertEquals(experiment.description(), experimentFromBytes.description());
            Assert.assertEquals(experiment.id(), experimentFromBytes.id());
            Assert.assertEquals(experiment.status(), experimentFromBytes.status());

        }

    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#start(String, User)}
     * When: Scheduling an Experiment
     * Should: Generate a running id
     */
    @Test
    public void scheduleExperiment_shouldGenerateRunningId() throws DotDataException, DotSecurityException {
        final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
        final Experiment experiment =  new ExperimentDataGen()
                .scheduling(Scheduling.builder().startDate(startDate).build())
                .nextPersisted();

        APILocator.getExperimentsAPI().start(experiment.id().orElseThrow(), APILocator.systemUser());

        try {
            final Experiment experimentAfterScheduled = APILocator.getExperimentsAPI()
                    .find(experiment.id().orElseThrow(), APILocator.systemUser())
                    .orElseThrow();

            assertFalse(experimentAfterScheduled.runningIds().getAll().isEmpty());

        } finally {
            final Experiment experimentFromDB = APILocator.getExperimentsAPI()
                    .find(experiment.getIdentifier(), APILocator.systemUser())
                    .orElseThrow();

            if (experimentFromDB.status() == Status.RUNNING) {
                APILocator.getExperimentsAPI().end(experiment.getIdentifier(), APILocator.systemUser());
            }
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#deleteVariant(String, String, User)}
     * When: Deleting the last Variant that is not the Original/Default
     * Should: Reset the split type to SPLIT_EVENLY
     */
    @Test
    public void removeAllVariants_ShouldResetSplitTypeToSplitEvenly()
            throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment experiment = new ExperimentDataGen()
                .page(pageA).nextPersisted();

        final SortedSet<ExperimentVariant> variants = experiment.trafficProportion().variants();
        final ExperimentVariant original80 = ExperimentVariant.builder()
                .from(variants.first()).weight(80f).build();

        final Experiment withVariant = APILocator.getExperimentsAPI()
                .addVariant(experiment.id().orElseThrow(), "v1",
                APILocator.systemUser());

        final ExperimentVariant v1 = withVariant.trafficProportion().variants().stream()
                .filter((variant)->variant.description().equals("v1"))
                .collect(Collectors.toList()).get(0);

        final SortedSet<ExperimentVariant> weightedVariants = new TreeSet<>();
        weightedVariants.add(original80);
        weightedVariants.add(ExperimentVariant.builder().from(v1)
                .weight(20)
                .build());

        final Experiment experimentWithSplitCustom = experiment.withTrafficProportion(
                experiment.trafficProportion().withVariants(weightedVariants).withType(Type.CUSTOM_PERCENTAGES));

        final Experiment persistedExperiment = APILocator.getExperimentsAPI()
                .save(experimentWithSplitCustom, APILocator.systemUser());

        APILocator.getExperimentsAPI().deleteVariant(persistedExperiment.id().orElseThrow(),
                v1.id(), APILocator.systemUser());

        final Experiment experimentAfterDelete = APILocator.getExperimentsAPI().
                find(persistedExperiment.id().orElseThrow(), APILocator.systemUser()).orElseThrow();

        assertEquals(Type.SPLIT_EVENLY, experimentAfterDelete.trafficProportion().type());
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#end(String, User)}
     * WHen: End An Experiment
     * Should: Archive the Variants
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void archiveVariantAfterStopExperiment() throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset pageA = new HTMLPageDataGen(host, template).nextPersisted();

        Experiment experiment = new ExperimentDataGen()
                .page(pageA).nextPersisted();

        experiment = APILocator.getExperimentsAPI()
                .addVariant(experiment.id().orElseThrow(), "v1", APILocator.systemUser());


        experiment = APILocator.getExperimentsAPI()
                .addVariant(experiment.id().orElseThrow(), "v2", APILocator.systemUser());

        final ExperimentVariant v1 = experiment.trafficProportion().variants().stream()
                .filter((variant)->variant.description().equals("v1"))
                .collect(Collectors.toList()).get(0);

        final ExperimentVariant v2 = experiment.trafficProportion().variants().stream()
                .filter((variant)->variant.description().equals("v2"))
                .collect(Collectors.toList()).get(0);

        APILocator.getExperimentsAPI().start(experiment.id().get(), APILocator.systemUser());
        APILocator.getExperimentsAPI().end(experiment.id().get(), APILocator.systemUser());

        final Variant variant1FromDataBase = APILocator.getVariantAPI().get(v1.id()).orElseThrow();
        assertTrue("The Variant must be archived", variant1FromDataBase.archived());


        final Variant variant2FromDataBase = APILocator.getVariantAPI().get(v2.id()).orElseThrow();
        assertTrue("The Variant must be archived", variant2FromDataBase.archived());

    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#listActive(String)}
     * When: The method is called
     * Should: use the {@link ExperimentsFactory#listActive(String)}
     */
    @Test
    public void listActive() throws DotDataException {
        final List<Experiment> experiments = new ArrayList<>();
        experiments.add(mock(Experiment.class));
        experiments.add(mock(Experiment.class));

        final Host host = mock(Host.class);

        final ExperimentsFactory experimentsFactory = mock();
        when(experimentsFactory.listActive(host.getIdentifier())).thenReturn(experiments);

        final ExperimentsAPI experimentsAPI = new ExperimentsAPIImpl(experimentsFactory);
        final Collection<Experiment> activeExperiments = experimentsAPI.listActive(host.getIdentifier());

        assertEquals(experiments, activeExperiments);
        verify(experimentsFactory).listActive(host.getIdentifier());
    }
}

