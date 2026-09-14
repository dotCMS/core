package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * Provider-agnostic experiment result query. Replaces the two CubeJS-specific factory methods
 * ({@code create} / {@code createWithDayGranularity}) with backend-neutral equivalents.
 * <p>
 * Implementations must populate {@link com.dotcms.analytics.model.ResultSetItem} fields using
 * the {@code Events.*} naming convention expected by the processing loops in
 * {@code ExperimentsAPIImpl.getResults()}.
 * </p>
 * <p>
 * The {@code user} parameter is required by the CubeJS path for client authentication.
 * CAEM implementations use host-based HMAC auth and may ignore this parameter.
 * </p>
 *
 * @see ExperimentResultsQueryFactory
 */
public interface ExperimentGoalResultsQuery {

    /**
     * Returns per-variant results broken down by day.
     * <p>
     * Replaces {@link ExperimentResultsQueryFactory#createWithDayGranularity(Experiment)}.
     * Used by {@code ExperimentsAPIImpl.getSummary()}.
     * </p>
     * <p>
     * Each {@link com.dotcms.analytics.model.ResultSetItem} must contain:
     * {@code Events.variant}, {@code Events.day}, {@code Events.totalSessions},
     * {@code Events.*Successes}, {@code Events.*ConversionRate}.
     * </p>
     *
     * @param experiment the experiment to query results for
     * @param user       calling user — used by the CubeJS path for client auth; CAEM implementations may ignore
     * @return per-day per-variant analytics result set
     * @throws DotDataException     if the underlying data source cannot be reached or returns an error
     * @throws DotSecurityException if the user lacks access to the analytics backend
     */
    AnalyticsResultSet executeByDay(Experiment experiment, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Returns aggregate (non-day) per-variant totals for <strong>all</strong> experiment variants.
     * <p>
     * Replaces {@link ExperimentResultsQueryFactory#create(Experiment)}.
     * Used by {@code ExperimentsAPIImpl.getTotalSessions()}.
     * </p>
     * <p>
     * Each {@link com.dotcms.analytics.model.ResultSetItem} must contain:
     * {@code Events.variant}, {@code Events.totalSessions},
     * {@code Events.*Successes}, {@code Events.*ConversionRate}.
     * </p>
     * <p>
     * <strong>Invariant</strong>: this method must return one row per experiment variant so that
     * {@code ExperimentResults.getSessions().getVariants().size() >= 2} holds, preserving the
     * Bayesian calculation gate in {@code ExperimentsAPIImpl}.
     * </p>
     *
     * @param experiment the experiment to query results for
     * @param user       calling user — used by the CubeJS path for client auth; CAEM implementations may ignore
     * @return aggregate per-variant analytics result set
     * @throws DotDataException     if the underlying data source cannot be reached or returns an error
     * @throws DotSecurityException if the user lacks access to the analytics backend
     */
    AnalyticsResultSet executeAggregate(Experiment experiment, User user)
            throws DotDataException, DotSecurityException;

}
