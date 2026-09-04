package com.dotcms.experiments.business.result;

import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.RunningIds;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BounceRateCAEMResultQuery}.
 *
 * Verifies param construction, response parsing, empty-result handling, and error surfacing.
 * {@link CaemHttpClient} is mocked — no real CAEM service required.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BounceRateCAEMResultQueryTest {

    private static final String EXPERIMENT_ID = "exp-123";
    private static final String RUNNING_ID    = "run-456";

    @Mock private CaemHttpClient caemHttpClient;
    @Mock private Experiment     experiment;
    @Mock private User           user;

    private BounceRateCAEMResultQuery query;

    @Before
    public void setUp() {
        query = new BounceRateCAEMResultQuery(caemHttpClient);

        when(experiment.getIdentifier()).thenReturn(EXPERIMENT_ID);
        when(experiment.status()).thenReturn(Status.RUNNING);

        final RunningIds.RunningId runningId = org.mockito.Mockito.mock(RunningIds.RunningId.class);
        when(runningId.id()).thenReturn(RUNNING_ID);
        final RunningIds runningIds = org.mockito.Mockito.mock(RunningIds.class);
        when(runningIds.getCurrent()).thenReturn(Optional.of(runningId));
        when(experiment.runningIds()).thenReturn(runningIds);
    }

    // -------------------------------------------------------------------------
    // executeByDay — param construction
    // -------------------------------------------------------------------------

    @Test
    public void executeByDay_includesDayDimension() throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(emptyResultSet());

        query.executeByDay(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions"),
                argThat(params ->
                        "variant,day".equals(params.get("dimensions"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))
                        && params.get("metrics").contains("bounceSessions")
                        && params.get("metrics").contains("bounceRate")
                        && params.get("metrics").contains("totalSessions")),
                any());
    }

    // -------------------------------------------------------------------------
    // executeAggregate — param construction
    // -------------------------------------------------------------------------

    @Test
    public void executeAggregate_excludesDayDimension() throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(emptyResultSet());

        query.executeAggregate(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions"),
                argThat(params ->
                        "variant".equals(params.get("dimensions"))
                        && EXPERIMENT_ID.equals(params.get("experimentId"))
                        && RUNNING_ID.equals(params.get("runningId"))
                        && params.get("metrics").contains("bounceSessions")),
                any());
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    @Test
    public void executeByDay_mapsResponseFieldsToEventsConvention() throws DotDataException, DotSecurityException {
        final AnalyticsResultSet caemResult = resultSetOf(Map.of(
                "Events.variant",                "control",
                "Events.day",                    "2026-09-01",
                "Events.totalSessions",          100L,
                "Events.bounceRateSuccesses",    45L,
                "Events.bounceRateConversionRate", 45.0f
        ));
        when(caemHttpClient.get(any(), any(), any())).thenReturn(caemResult);

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(1, result.size());
        final ResultSetItem item = result.iterator().next();
        assertEquals("control", item.get("Events.variant").orElseThrow());
        assertEquals("2026-09-01", item.get("Events.day").orElseThrow());
        assertEquals(100L, item.get("Events.totalSessions").orElseThrow());
        assertEquals(45L, item.get("Events.bounceRateSuccesses").orElseThrow());
    }

    // -------------------------------------------------------------------------
    // Empty result (FR-014)
    // -------------------------------------------------------------------------

    @Test
    public void executeByDay_emptyCAEMResponse_returnsZeroRows() throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(emptyResultSet());

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void executeAggregate_emptyCAEMResponse_returnsZeroRows() throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(emptyResultSet());

        final AnalyticsResultSet result = query.executeAggregate(experiment, user);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // -------------------------------------------------------------------------
    // Error surfacing (FR-017)
    // -------------------------------------------------------------------------

    @Test(expected = DotDataException.class)
    public void executeByDay_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any()))
                .thenThrow(new DotDataException("CAEM 503"));

        query.executeByDay(experiment, user);
    }

    @Test(expected = DotDataException.class)
    public void executeAggregate_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any()))
                .thenThrow(new DotDataException("CAEM 503"));

        query.executeAggregate(experiment, user);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AnalyticsResultSet emptyResultSet() {
        return new com.dotcms.cube.AnalyticsResultSetImpl(java.util.Collections.emptyList());
    }

    private static AnalyticsResultSet resultSetOf(final Map<String, Object> fields) {
        return new com.dotcms.cube.AnalyticsResultSetImpl(java.util.List.of(new java.util.HashMap<>(fields)));
    }

}
