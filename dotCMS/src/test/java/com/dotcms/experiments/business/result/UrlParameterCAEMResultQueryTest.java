package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.analytics.metrics.QueryParameter;
import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.AnalyticsResultSetImpl;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.RunningIds;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.dotcms.analytics.model.ResultSetItem;
import static org.junit.Assert.assertNotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UrlParameterCAEMResultQuery}.
 *
 * <p>Covers the following behaviours (see FR-006, FR-006a, FR-006b, FR-014, FR-017):</p>
 * <ul>
 *   <li><strong>executeByDay param construction</strong> — verifies that the CAEM request targets
 *       {@code /v1/analytics/sessions/behavior} with {@code behavior=urlParam},
 *       {@code dimensions=variant,day}, the correct {@code experimentId} / {@code runningId},
 *       {@code paramName} sourced from {@link com.dotcms.analytics.metrics.QueryParameter#getName()},
 *       and {@code paramValue} sourced from {@link com.dotcms.analytics.metrics.QueryParameter#getValue()}
 *       on the goal's {@code "queryParameter"} condition.</li>
 *   <li><strong>executeAggregate param construction</strong> — same params but
 *       {@code dimensions=variant} only (no day granularity).</li>
 *   <li><strong>Empty result (FR-014)</strong> — a zero-row CAEM response produces an empty
 *       {@link com.dotcms.cube.AnalyticsResultSet} without throwing an exception. Note:
 *       "any event matches" semantics are enforced by CAEM and are not verified here.</li>
 *   <li><strong>Error surfacing (FR-017)</strong> — a {@link com.dotmarketing.exception.DotDataException}
 *       thrown by {@link CaemHttpClient} is propagated to the caller without being swallowed.</li>
 * </ul>
 *
 * <p>{@link CaemHttpClient} is mocked — no real CAEM service or dotCMS container is required.</p>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UrlParameterCAEMResultQueryTest {

    private static final String EXPERIMENT_ID = "exp-url-123";
    private static final String RUNNING_ID    = "run-url-456";
    private static final String PARAM_NAME    = "converted";
    private static final String PARAM_VALUE   = "true";

    @Mock private CaemHttpClient caemHttpClient;
    @Mock private Experiment     experiment;
    @Mock private User           user;

    private UrlParameterCAEMResultQuery query;

    @Before
    public void setUp() {
        query = new UrlParameterCAEMResultQuery(caemHttpClient);

        when(experiment.getIdentifier()).thenReturn(EXPERIMENT_ID);
        when(experiment.status()).thenReturn(Status.RUNNING);

        final RunningIds.RunningId runningId = mock(RunningIds.RunningId.class);
        when(runningId.id()).thenReturn(RUNNING_ID);
        final RunningIds runningIds = mock(RunningIds.class);
        when(runningIds.getCurrent()).thenReturn(Optional.of(runningId));
        when(experiment.runningIds()).thenReturn(runningIds);

        final QueryParameter qp = new QueryParameter(PARAM_NAME, PARAM_VALUE);
        final Metric metric = Metric.builder()
                .name("URL param test")
                .type(MetricType.URL_PARAMETER)
                .addConditions(
                        Condition.builder()
                                .parameter("queryParameter")
                                .value(qp)
                                .operator(Operator.EQUALS)
                                .build()
                ).build();
        final Goals goals = Goals.builder().primary(GoalFactory.create(metric)).build();
        when(experiment.goals()).thenReturn(Optional.of(goals));
    }

    /**
     * Method to test: {@link UrlParameterCAEMResultQuery#executeByDay(Experiment, User)}
     * When: called with a running experiment with a URL-parameter goal
     * Should: call {@code GET /v1/analytics/sessions/behavior} with {@code behavior=urlParam},
     *         {@code paramName} and {@code paramValue} from the goal's {@code QueryParameter}
     *         condition, and {@code dimensions=variant,day}
     */
    @Test
    public void executeByDay_includesBehaviorParamNameParamValue()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeByDay(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions/behavior"),
                argThat(params ->
                        "urlParam".equals(params.get("behavior"))
                        && PARAM_NAME.equals(params.get("paramName"))
                        && PARAM_VALUE.equals(params.get("paramValue"))
                        && "variant,day".equals(params.get("dimensions"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))),
                any());
    }

    /**
     * Method to test: {@link UrlParameterCAEMResultQuery#executeAggregate(Experiment, User)}
     * When: called with a running experiment
     * Should: call the behavior endpoint with {@code dimensions=variant} only (no day granularity)
     */
    @Test
    public void executeAggregate_excludesDayDimension()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeAggregate(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions/behavior"),
                argThat(params ->
                        "variant".equals(params.get("dimensions"))
                        && "urlParam".equals(params.get("behavior"))
                        && PARAM_NAME.equals(params.get("paramName"))
                        && PARAM_VALUE.equals(params.get("paramValue"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))),
                any());
    }

    /**
     * Method to test: {@link UrlParameterCAEMResultQuery#executeByDay(Experiment, User)}
     * When: CAEM returns a row with variant, day, and URL-parameter metrics
     * Should: map CAEM fields to the {@code Events.*} naming convention expected by the
     *         {@code ExperimentsAPIImpl} processing loops
     */
    @Test
    public void executeByDay_mapsResponseFieldsToEventsConvention()
            throws DotDataException, DotSecurityException {
        final AnalyticsResultSet caemResult = resultSetOf(Map.of(
                "Events.variant",                    "control",
                "Events.day",                        "2026-09-01",
                "Events.totalSessions",              80L,
                "Events.reachPageRateSuccesses",     32L,
                "Events.reachPageRateConversionRate", 40.0f
        ));
        when(caemHttpClient.get(any(), any(), any())).thenReturn(caemResult);

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(1, result.size());
        final ResultSetItem item = result.iterator().next();
        assertEquals("control", item.get("Events.variant").orElseThrow());
        assertEquals("2026-09-01", item.get("Events.day").orElseThrow());
        assertEquals(80L, item.get("Events.totalSessions").orElseThrow());
        assertEquals(32L, item.get("Events.reachPageRateSuccesses").orElseThrow());
    }

    /**
     * Method to test: {@link UrlParameterCAEMResultQuery#executeByDay(Experiment, User)}
     * When: CAEM returns zero rows (no sessions contained the target URL parameter)
     * Should: return an empty {@link AnalyticsResultSet} — not an error and not {@code null}.
     *         Note: "any event matches" semantics are enforced by CAEM and are not tested here.
     */
    @Test
    public void executeByDay_emptyResponse_returnsZeroRows()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());
        assertEquals(0, query.executeByDay(experiment, user).size());
    }

    /**
     * Method to test: {@link UrlParameterCAEMResultQuery#executeByDay(Experiment, User)}
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
     * Method to test: {@link UrlParameterCAEMResultQuery#executeAggregate(Experiment, User)}
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
