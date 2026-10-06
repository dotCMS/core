package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSClientFactory;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import graphql.VisibleForTesting;

/**
 * Temporary shim that implements {@link ExperimentGoalResultsQuery} by delegating to an
 * existing {@link MetricExperimentResultsQuery} implementation and executing the resulting
 * {@link com.dotcms.cube.CubeJSQuery} against the CubeJS client.
 * <p>
 * This class is designed for clean deletion when the CubeJS infrastructure is removed — only
 * this adapter needs to be deleted; the CAEM implementations and factory dispatch logic require
 * no changes at that point.
 * </p>
 * <p>
 * CubeJS handles the {@link com.dotcms.experiments.model.Goal.GoalType#MINIMIZE} inversion
 * server-side via its cube schema definitions (e.g. {@code bounceRateSuccesses} already
 * represents non-bounced sessions). Therefore this adapter overrides
 * {@link #executeAggregate} and {@link #executeByDay} directly — bypassing the template in
 * {@link ExperimentGoalResultsQuery} — so that the client-side inversion is not applied twice.
 * </p>
 *
 * @see ExperimentGoalResultsQuery
 * @see ExperimentResultsQueryFactory
 */
public class CubeJSGoalResultsAdapter implements ExperimentGoalResultsQuery {

    private final MetricExperimentResultsQuery metricQuery;

    public CubeJSGoalResultsAdapter(final MetricExperimentResultsQuery metricQuery) {
        this.metricQuery = metricQuery;
    }

    /**
     * Raw per-day CubeJS fetch — satisfies the abstract contract; not used directly by callers.
     */
    @Override
    public AnalyticsResultSet doExecuteByDay(final Experiment experiment,
                                             final User user) throws DotDataException, DotSecurityException {
        final CubeJSClient client = cubeJSClientFactory().create(user);
        return client.send(ExperimentResultsQueryFactory.INSTANCE.buildDayGranularityQuery(experiment, metricQuery));
    }

    /**
     * Raw aggregate CubeJS fetch — satisfies the abstract contract; not used directly by callers.
     */
    @Override
    public AnalyticsResultSet doExecuteAggregate(final Experiment experiment,
                                                 final User user) throws DotDataException, DotSecurityException {
        final CubeJSClient client = cubeJSClientFactory().create(user);
        return client.send(ExperimentResultsQueryFactory.INSTANCE.buildAggregateQuery(experiment, metricQuery));
    }

    /**
     * Bypasses the MINIMIZE template — CubeJS already returns correctly inverted values.
     */
    @Override
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        return doExecuteByDay(experiment, user);
    }

    /**
     * Bypasses the MINIMIZE template — CubeJS already returns correctly inverted values.
     */
    @Override
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        return doExecuteAggregate(experiment, user);
    }

    /**
     * Returns the wrapped {@link MetricExperimentResultsQuery} for test inspection.
     */
    @VisibleForTesting
    public MetricExperimentResultsQuery getMetricQuery() {
        return metricQuery;
    }

    private static CubeJSClientFactory cubeJSClientFactory() {
        return FactoryLocator.getCubeJSClientFactory();
    }

}