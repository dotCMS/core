package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.AnalyticsResultSetImpl;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.RunningIds;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.dotcms.analytics.model.ResultSetItem;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReachPageCAEMResultQuery}.
 *
 * <p>Covers the following behaviours (see FR-005, FR-006a, FR-006b, FR-014, FR-017):</p>
 * <ul>
 *   <li><strong>executeByDay param construction</strong> — verifies that the CAEM request targets
 *       {@code /v1/sessions/behavior} with {@code behavior=reachTarget},
 *       {@code dimensions=variant,day}, the correct {@code experimentId} / {@code runningId},
 *       {@code referencePage} resolved from the experiment page (via {@code APILocator}), and
 *       {@code targetUrl} extracted from the {@code "url"} goal condition.</li>
 *   <li><strong>executeAggregate param construction</strong> — same params but
 *       {@code dimensions=variant} only (no day granularity).</li>
 *   <li><strong>Empty result (FR-014)</strong> — a zero-row CAEM response produces an empty
 *       {@link com.dotcms.cube.AnalyticsResultSet} without throwing an exception. Note: session
 *       ordering (target visited after reference) is enforced by CAEM and is not verified here.</li>
 *   <li><strong>Error surfacing (FR-017)</strong> — a {@link com.dotmarketing.exception.DotDataException}
 *       thrown by {@link CaemHttpClient} is propagated to the caller without being swallowed.</li>
 * </ul>
 *
 * <p>{@link CaemHttpClient} is mocked — no real CAEM service or dotCMS container is required.
 * {@link APILocator} is stubbed via {@code mockStatic} so {@link GoalConditionUtil#resolvePageUri}
 * returns {@link #REFERENCE_PAGE} without a live database lookup.</p>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReachPageCAEMResultQueryTest {

    private static final String EXPERIMENT_ID  = "exp-reach-123";
    private static final String RUNNING_ID     = "run-reach-456";
    private static final String PAGE_ID        = "page-reach-789";
    private static final String REFERENCE_PAGE = "/landing";
    private static final String TARGET_URL     = "/thank-you";

    @Mock private CaemHttpClient   caemHttpClient;
    @Mock private Experiment       experiment;
    @Mock private User             user;
    @Mock private ContentletAPI    contentletAPI;
    @Mock private HTMLPageAssetAPI htmlPageAssetAPI;
    @Mock private HTMLPageAsset    experimentPage;
    @Mock private Contentlet       pageContentlet;

    private ReachPageCAEMResultQuery query;
    private MockedStatic<APILocator> mockedApiLocator;

    @Before
    public void setUp() throws Exception {
        query = new ReachPageCAEMResultQuery(caemHttpClient);

        when(experiment.getIdentifier()).thenReturn(EXPERIMENT_ID);
        when(experiment.status()).thenReturn(Status.RUNNING);
        when(experiment.pageId()).thenReturn(PAGE_ID);

        final RunningIds.RunningId runningId = mock(RunningIds.RunningId.class);
        when(runningId.id()).thenReturn(RUNNING_ID);
        final RunningIds runningIds = mock(RunningIds.class);
        when(runningIds.getCurrent()).thenReturn(Optional.of(runningId));
        when(experiment.runningIds()).thenReturn(runningIds);

        final Metric metric = Metric.builder()
                .name("Reach test")
                .type(MetricType.REACH_PAGE)
                .addConditions(
                        Condition.builder().parameter("url").value(TARGET_URL).operator(Operator.EQUALS).build()
                ).build();
        final Goals goals = Goals.builder().primary(GoalFactory.create(metric)).build();
        when(experiment.goals()).thenReturn(Optional.of(goals));

        // Stub APILocator so resolvePageUri works without a live dotCMS container.
        mockedApiLocator = mockStatic(APILocator.class);
        mockedApiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        mockedApiLocator.when(APILocator::getHTMLPageAssetAPI).thenReturn(htmlPageAssetAPI);
        when(contentletAPI.findContentletByIdentifierAnyLanguage(PAGE_ID, false)).thenReturn(pageContentlet);
        when(htmlPageAssetAPI.fromContentlet(pageContentlet)).thenReturn(experimentPage);
        when(experimentPage.getURI()).thenReturn(REFERENCE_PAGE);
    }

    @After
    public void tearDown() {
        mockedApiLocator.close();
    }

    // -------------------------------------------------------------------------
    // Param construction
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ReachPageCAEMResultQuery#executeByDay(Experiment, User)}
     * When: called with a running experiment with a reach-target goal
     * Should: call {@code GET /v1/sessions/behavior} with {@code behavior=reachTarget},
     *         {@code referencePage} resolved from the experiment page,
     *         {@code targetUrl} from the {@code "url"} condition, and {@code dimensions=variant,day}
     */
    @Test
    public void executeByDay_includesBehaviorReferencePageTargetUrl()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeByDay(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/sessions/behavior"),
                argThat(params ->
                        "reachTarget".equals(params.get("behavior"))
                        && REFERENCE_PAGE.equals(params.get("referencePage"))
                        && TARGET_URL.equals(params.get("targetUrl"))
                        && "variant,day".equals(params.get("dimensions"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))),
                any());
    }

    /**
     * Method to test: {@link ReachPageCAEMResultQuery#executeAggregate(Experiment, User)}
     * When: called with a running experiment
     * Should: call the behavior endpoint with {@code dimensions=variant} only (no day granularity)
     */
    @Test
    public void executeAggregate_excludesDayDimension()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeAggregate(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/sessions/behavior"),
                argThat(params ->
                        "variant".equals(params.get("dimensions"))
                        && "reachTarget".equals(params.get("behavior"))
                        && REFERENCE_PAGE.equals(params.get("referencePage"))
                        && TARGET_URL.equals(params.get("targetUrl"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))),
                any());
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ReachPageCAEMResultQuery#executeByDay(Experiment, User)}
     * When: CAEM returns a row with variant, day, and reach-target metrics
     * Should: map CAEM fields to the {@code Events.*} naming convention expected by the
     *         {@code ExperimentsAPIImpl} processing loops
     */
    @Test
    public void executeByDay_mapsResponseFieldsToEventsConvention()
            throws DotDataException, DotSecurityException {
        final AnalyticsResultSet caemResult = resultSetOf(Map.of(
                "Events.variant",                    "control",
                "Events.day",                        "2026-09-01",
                "Events.totalSessions",              50L,
                "Events.reachPageRateSuccesses",     15L,
                "Events.reachPageRateConversionRate", 30.0f
        ));
        when(caemHttpClient.get(any(), any(), any())).thenReturn(caemResult);

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(1, result.size());
        final ResultSetItem item = result.iterator().next();
        assertEquals("control", item.get("Events.variant").orElseThrow());
        assertEquals("2026-09-01", item.get("Events.day").orElseThrow());
        assertEquals(50L, item.get("Events.totalSessions").orElseThrow());
        assertEquals(15L, item.get("Events.reachPageRateSuccesses").orElseThrow());
    }

    // -------------------------------------------------------------------------
    // Empty result (FR-014)
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ReachPageCAEMResultQuery#executeByDay(Experiment, User)}
     * When: CAEM returns zero rows (no sessions reached the target after the reference page)
     * Should: return an empty {@link AnalyticsResultSet} — not an error and not {@code null}.
     *         Note: session ordering is enforced by CAEM and is not tested here.
     */
    @Test
    public void executeByDay_emptyResponse_returnsZeroRows()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());
        assertEquals(0, query.executeByDay(experiment, user).size());
    }

    // -------------------------------------------------------------------------
    // Error surfacing (FR-017)
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ReachPageCAEMResultQuery#executeByDay(Experiment, User)}
     * When: {@link CaemHttpClient} throws a {@link com.dotmarketing.exception.DotDataException}
     * Should: propagate the exception to the caller — never swallow it silently
     */
    @Test(expected = DotDataException.class)
    public void executeByDay_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenThrow(new DotDataException("503"));
        query.executeByDay(experiment, user);
    }

    /**
     * Method to test: {@link ReachPageCAEMResultQuery#executeAggregate(Experiment, User)}
     * When: {@link CaemHttpClient} throws a {@link com.dotmarketing.exception.DotDataException}
     * Should: propagate the exception to the caller — never swallow it silently
     */
    @Test(expected = DotDataException.class)
    public void executeAggregate_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenThrow(new DotDataException("503"));
        query.executeAggregate(experiment, user);
    }

    private static AnalyticsResultSet empty() {
        return new AnalyticsResultSetImpl(Collections.emptyList());
    }

    private static AnalyticsResultSet resultSetOf(final Map<String, Object> fields) {
        return new AnalyticsResultSetImpl(java.util.List.of(new java.util.HashMap<>(fields)));
    }

}