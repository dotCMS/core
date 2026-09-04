package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.Map;

/**
 * Factory for experiment result queries. Dispatches each call to either the CubeJS or CAEM
 * implementation based on {@code FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS}, read per call via
 * {@link ConfigExperimentUtil#isCaemExperimentResultsEnabled()} so flag changes take effect
 * immediately without a restart.
 *
 * <p>Two dispatch methods ({@link #executeByDay} / {@link #executeAggregate}) replace the old
 * {@link #create(Experiment)} / {@link #createWithDayGranularity(Experiment)} pair,
 * which are now deprecated but retained for use by {@link CubeJSGoalResultsAdapter}.</p>
 *
 * @see ExperimentGoalResultsQuery
 * @see CubeJSGoalResultsAdapter
 */
public enum ExperimentResultsQueryFactory {

    INSTANCE;

    // CubeJS path — one adapter per goal type, wrapping existing MetricExperimentResultsQuery implementations.
    private final Map<MetricType, ExperimentGoalResultsQuery> cubeJSAdapters;

    // CAEM path — one implementation per goal type (shared CaemHttpClient instance).
    private final Map<MetricType, ExperimentGoalResultsQuery> caemQueries;

    ExperimentResultsQueryFactory() {
        cubeJSAdapters = Map.of(
            MetricType.BOUNCE_RATE,   new CubeJSGoalResultsAdapter(new BounceRateResultQuery()),
            MetricType.EXIT_RATE,     new CubeJSGoalResultsAdapter(new ExitRateResultQuery()),
            MetricType.REACH_PAGE,    new CubeJSGoalResultsAdapter(new ReachTargetAfterExperimentPageResultQuery()),
            MetricType.URL_PARAMETER, new CubeJSGoalResultsAdapter(new ReachTargetAfterExperimentPageResultQuery())
        );

        final CaemHttpClient caemHttpClient = new CaemHttpClient();
        caemQueries = Map.of(
            MetricType.BOUNCE_RATE,   new BounceRateCAEMResultQuery(caemHttpClient),
            MetricType.EXIT_RATE,     new ExitRateCAEMResultQuery(caemHttpClient),
            MetricType.REACH_PAGE,    new ReachPageCAEMResultQuery(caemHttpClient),
            MetricType.URL_PARAMETER, new UrlParameterCAEMResultQuery(caemHttpClient)
        );
    }

    /**
     * Returns per-day per-variant results. Replaces {@link #createWithDayGranularity(Experiment)}
     * as the primary call site in {@code ExperimentsAPIImpl.getSummary()}.
     */
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        return resolveImpl(primaryMetricType(experiment)).executeByDay(experiment, user);
    }

    /**
     * Returns aggregate per-variant totals. Replaces {@link #create(Experiment)}
     * as the primary call site in {@code ExperimentsAPIImpl.getTotalSessions()}.
     */
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        return resolveImpl(primaryMetricType(experiment)).executeAggregate(experiment, user);
    }

    /**
     * Resolves the correct {@link ExperimentGoalResultsQuery} for the given metric type,
     * reading the feature flag on every call so runtime toggles take effect immediately.
     */
    public ExperimentGoalResultsQuery resolveImpl(final MetricType metricType) {
        return ConfigExperimentUtil.INSTANCE.isCaemExperimentResultsEnabled()
                ? caemQueries.get(metricType)
                : cubeJSAdapters.get(metricType);
    }

    // -------------------------------------------------------------------------
    // Package-private helpers used by CubeJSGoalResultsAdapter
    // -------------------------------------------------------------------------

    CubeJSQuery buildDayGranularityQuery(final Experiment experiment,
                                         final MetricExperimentResultsQuery metricQuery) {
        return CubeJSQuery.Builder.merge(
                metricQuery.getCubeJSQuery(experiment),
                createRootQuery(experiment, true));
    }

    CubeJSQuery buildAggregateQuery(final Experiment experiment,
                                    final MetricExperimentResultsQuery metricQuery) {
        return CubeJSQuery.Builder.merge(
                metricQuery.getCubeJSQuery(experiment),
                createRootQuery(experiment, false));
    }

    // -------------------------------------------------------------------------
    // Deprecated — retained for backward compatibility only
    // -------------------------------------------------------------------------

    /**
     * @deprecated Use {@link #executeByDay(Experiment, User)} instead.
     */
    @Deprecated
    public CubeJSQuery createWithDayGranularity(final Experiment experiment) {
        return buildDayGranularityQuery(experiment, legacyCubeJSMetricQuery(experiment));
    }

    /**
     * @deprecated Use {@link #executeAggregate(Experiment, User)} instead.
     */
    @Deprecated
    public CubeJSQuery create(final Experiment experiment) {
        return buildAggregateQuery(experiment, legacyCubeJSMetricQuery(experiment));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static MetricType primaryMetricType(final Experiment experiment) {
        DotPreconditions.notNull(experiment.goals().orElse(null), "Experiment must have a Goal");
        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("Experiment must have a Goal"));
        return goals.primary().getMetric().type();
    }

    private static MetricExperimentResultsQuery legacyCubeJSMetricQuery(final Experiment experiment) {
        return switch (primaryMetricType(experiment)) {
            case BOUNCE_RATE            -> new BounceRateResultQuery();
            case EXIT_RATE              -> new ExitRateResultQuery();
            case REACH_PAGE,
                 URL_PARAMETER         -> new ReachTargetAfterExperimentPageResultQuery();
            default -> throw new IllegalArgumentException(
                    "Unsupported metric type: " + primaryMetricType(experiment));
        };
    }

    static CubeJSQuery createRootQuery(final Experiment experiment,
                                       final boolean dayGranularity) {
        DotPreconditions.isTrue(
                experiment.status() == Status.RUNNING || experiment.status() == Status.ENDED,
                "Experiment must be running or Ended");

        final String runningId = experiment.runningIds().getCurrent().orElseThrow().id();

        final CubeJSQuery.Builder builder = new CubeJSQuery.Builder()
                .dimensions("Events.variant")
                .order("Events.day", Filter.Order.ASC)
                .filter("Events.experiment", Operator.EQUALS, experiment.getIdentifier())
                .filter("Events.runningId", Operator.EQUALS, runningId);

        if (dayGranularity) {
            builder.timeDimension("Events.day", "day");
        }

        return builder.build();
    }

}
