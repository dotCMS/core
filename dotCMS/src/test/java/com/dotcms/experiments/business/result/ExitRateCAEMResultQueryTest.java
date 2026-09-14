package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.AnalyticsResultSetImpl;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExitRateCAEMResultQuery}.
 *
 * <p>Covers the following behaviours (see FR-004, FR-006a, FR-006b, FR-014, FR-017):</p>
 * <ul>
 *   <li><strong>executeByDay param construction</strong> — verifies that the CAEM request includes
 *       {@code metrics=totalSessions,exitSessions,exitRate}, {@code dimensions=variant,day}, and the
 *       correct {@code experimentId} / {@code runningId}.</li>
 *   <li><strong>executeAggregate param construction</strong> — same metrics but {@code dimensions=variant}
 *       only (no day granularity).</li>
 *   <li><strong>Empty result (FR-014)</strong> — a zero-row CAEM response (e.g. no sessions exited on
 *       the configured reference page) produces an empty {@link com.dotcms.cube.AnalyticsResultSet}
 *       without throwing an exception.</li>
 *   <li><strong>Error surfacing (FR-017)</strong> — a {@link com.dotmarketing.exception.DotDataException}
 *       thrown by {@link CaemHttpClient} is propagated to the caller without being swallowed.</li>
 * </ul>
 *
 * <p>{@link CaemHttpClient} is mocked — no real CAEM service or dotCMS container is required.</p>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ExitRateCAEMResultQueryTest {

    private static final String EXPERIMENT_ID  = "exp-exit-123";
    private static final String RUNNING_ID     = "run-exit-456";

    @Mock private CaemHttpClient caemHttpClient;
    @Mock private Experiment     experiment;
    @Mock private User           user;

    private ExitRateCAEMResultQuery query;

    @Before
    public void setUp() {
        query = new ExitRateCAEMResultQuery(caemHttpClient);

        when(experiment.getIdentifier()).thenReturn(EXPERIMENT_ID);
        when(experiment.status()).thenReturn(Status.RUNNING);

        final RunningIds.RunningId runningId = mock(RunningIds.RunningId.class);
        when(runningId.id()).thenReturn(RUNNING_ID);
        final RunningIds runningIds = mock(RunningIds.class);
        when(runningIds.getCurrent()).thenReturn(Optional.of(runningId));
        when(experiment.runningIds()).thenReturn(runningIds);
    }

    // -------------------------------------------------------------------------
    // Param construction
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeByDay(Experiment, User)}
     * When: called with a running experiment
     * Should: call {@code GET /v1/analytics/sessions} with exit metrics,
     *         {@code dimensions=variant,day}, and the correct {@code experimentId} / {@code runningId}
     */
    @Test
    public void executeByDay_includesReferencePageAndDayDimension()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeByDay(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions"),
                argThat(params ->
                        "variant,day".equals(params.get("dimensions"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))
                        && params.get("metrics").contains("exitSessions")
                        && params.get("metrics").contains("exitRate")),
                any());
    }

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeAggregate(Experiment, User)}
     * When: called with a running experiment
     * Should: call {@code GET /v1/analytics/sessions} with {@code dimensions=variant} only
     *         (no day granularity) — used by the aggregate totals loop in {@code ExperimentsAPIImpl}
     */
    @Test
    public void executeAggregate_excludesDayDimension()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeAggregate(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions"),
                argThat(params ->
                        "variant".equals(params.get("dimensions"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))
                        && params.get("metrics").contains("exitSessions")
                        && params.get("metrics").contains("exitRate")),
                any());
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeByDay(Experiment, User)}
     * When: CAEM returns a row with variant, day, and exit metrics
     * Should: map CAEM fields to the {@code Events.*} naming convention expected by the
     *         {@code ExperimentsAPIImpl} processing loops
     */
    @Test
    public void executeByDay_mapsResponseFieldsToEventsConvention()
            throws DotDataException, DotSecurityException {
        final AnalyticsResultSet caemResult = resultSetOf(Map.of(
                "Events.variant",              "control",
                "Events.day",                  "2026-09-01",
                "Events.totalSessions",        100L,
                "Events.exitRateSuccesses",    20L,
                "Events.exitRateConversionRate", 20.0f
        ));
        when(caemHttpClient.get(any(), any(), any())).thenReturn(caemResult);

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(1, result.size());
        final ResultSetItem item = result.iterator().next();
        assertEquals("control", item.get("Events.variant").orElseThrow());
        assertEquals("2026-09-01", item.get("Events.day").orElseThrow());
        assertEquals(100L, item.get("Events.totalSessions").orElseThrow());
        assertEquals(20L, item.get("Events.exitRateSuccesses").orElseThrow());
    }

    // -------------------------------------------------------------------------
    // Empty result (FR-014)
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeByDay(Experiment, User)}
     * When: CAEM returns zero rows (no sessions exited on the configured reference page)
     * Should: return an empty {@link AnalyticsResultSet} — not an error and not {@code null}
     */
    @Test
    public void executeByDay_emptyCAEMResponse_returnsZeroRows()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeAggregate(Experiment, User)}
     * When: CAEM returns zero rows
     * Should: return an empty {@link AnalyticsResultSet} — not an error and not {@code null}
     */
    @Test
    public void executeAggregate_emptyCAEMResponse_returnsZeroRows()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        final AnalyticsResultSet result = query.executeAggregate(experiment, user);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // -------------------------------------------------------------------------
    // Error surfacing (FR-017)
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeByDay(Experiment, User)}
     * When: {@link CaemHttpClient} throws a {@link com.dotmarketing.exception.DotDataException}
     *       (e.g. non-2xx response or malformed body)
     * Should: propagate the exception to the caller — never swallow it silently
     */
    @Test(expected = DotDataException.class)
    public void executeByDay_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any()))
                .thenThrow(new DotDataException("CAEM 503"));
        query.executeByDay(experiment, user);
    }

    /**
     * Method to test: {@link ExitRateCAEMResultQuery#executeAggregate(Experiment, User)}
     * When: {@link CaemHttpClient} throws a {@link com.dotmarketing.exception.DotDataException}
     * Should: propagate the exception to the caller — never swallow it silently
     */
    @Test(expected = DotDataException.class)
    public void executeAggregate_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any()))
                .thenThrow(new DotDataException("CAEM 503"));
        query.executeAggregate(experiment, user);
    }

    private static AnalyticsResultSet empty() {
        return new AnalyticsResultSetImpl(Collections.emptyList());
    }

    private static AnalyticsResultSet resultSetOf(final Map<String, Object> fields) {
        return new AnalyticsResultSetImpl(java.util.List.of(new java.util.HashMap<>(fields)));
    }

}
