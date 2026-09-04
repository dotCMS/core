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

import java.util.Collections;
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

    @Test
    public void executeAggregate_excludesDayDimension()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());

        query.executeAggregate(experiment, user);

        verify(caemHttpClient).get(
                eq("/v1/analytics/sessions/behavior"),
                argThat(params -> "variant".equals(params.get("dimensions"))),
                any());
    }

    @Test
    public void executeByDay_emptyResponse_returnsZeroRows()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenReturn(empty());
        assertEquals(0, query.executeByDay(experiment, user).size());
    }

    @Test(expected = DotDataException.class)
    public void executeByDay_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenThrow(new DotDataException("503"));
        query.executeByDay(experiment, user);
    }

    @Test(expected = DotDataException.class)
    public void executeAggregate_caemClientThrows_propagatesDotDataException()
            throws DotDataException, DotSecurityException {
        when(caemHttpClient.get(any(), any(), any())).thenThrow(new DotDataException("503"));
        query.executeAggregate(experiment, user);
    }

    private static AnalyticsResultSet empty() {
        return new AnalyticsResultSetImpl(Collections.emptyList());
    }

}
