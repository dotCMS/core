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
     * Returns per-day per-variant results by executing {@code createWithDayGranularity()} against CubeJS.
     */
    @Override
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        final CubeJSClient client = cubeJSClientFactory().create(user);
        return client.send(ExperimentResultsQueryFactory.INSTANCE.buildDayGranularityQuery(experiment, metricQuery));
    }

    /**
     * Returns aggregate per-variant totals by executing {@code create()} against CubeJS.
     */
    @Override
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        final CubeJSClient client = cubeJSClientFactory().create(user);
        return client.send(ExperimentResultsQueryFactory.INSTANCE.buildAggregateQuery(experiment, metricQuery));
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
