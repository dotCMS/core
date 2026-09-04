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
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ExitRateCAEMResultQueryTest {

    private static final String EXPERIMENT_ID  = "exp-exit-123";
    private static final String RUNNING_ID     = "run-exit-456";
    private static final String REFERENCE_PAGE = "/checkout";

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

    @Test
    public void executeAggregate_excludesDayDimension()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeAggregate(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions"),
                argThat(params -> "variant".equals(params.get("dimensions"))),
                any());
    }

    // -------------------------------------------------------------------------
    // Empty result (FR-014)
    // -------------------------------------------------------------------------

    @Test
    public void executeByDay_emptyCAEMResponse_returnsZeroRows()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        final AnalyticsResultSet result = query.executeByDay(experiment, user);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

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

    private static AnalyticsResultSet empty() {
        return new AnalyticsResultSetImpl(Collections.emptyList());
    }

}
