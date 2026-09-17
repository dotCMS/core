package com.dotcms.experiments.business.result;

import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.AnalyticsResultSetImpl;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Goal;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <h3>Template method pattern</h3>
 * <p>
 * Implementations provide raw backend results via {@link #doExecuteAggregate} and
 * {@link #doExecuteByDay}. The public {@link #executeAggregate} and {@link #executeByDay}
 * default methods apply a goal-type adjustment automatically: for {@link Goal.GoalType#MINIMIZE}
 * goals the raw backend counts represent undesired events (e.g. bounces, exits), so successes
 * and conversion rate are inverted before the results reach the caller. This ensures that a new
 * {@code MINIMIZE} goal implementation never needs to handle the inversion manually.
 * </p>
 * <p>
 * Implementations whose backend already handles the inversion server-side (e.g.
 * {@link CubeJSGoalResultsAdapter}) must override {@link #executeAggregate} and
 * {@link #executeByDay} directly to bypass the template.
 * </p>
 *
 * @see ExperimentResultsQueryFactory
 */
public interface ExperimentGoalResultsQuery {

    /**
     * Raw per-day fetch — provides unadjusted backend results.
     * <p>
     * Each {@link ResultSetItem} must contain: {@code Events.variant}, {@code Events.day},
     * {@code Events.totalSessions}, {@code Events.*Successes}, {@code Events.*ConversionRate}.
     * </p>
     *
     * @param experiment the experiment to query results for
     * @param user       calling user — used by the CubeJS path for client auth; CAEM implementations may ignore
     * @return raw per-day per-variant analytics result set
     * @throws DotDataException     if the underlying data source cannot be reached or returns an error
     * @throws DotSecurityException if the user lacks access to the analytics backend
     */
    AnalyticsResultSet doExecuteByDay(Experiment experiment, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Raw aggregate fetch — provides unadjusted backend results.
     * <p>
     * Each {@link ResultSetItem} must contain: {@code Events.variant},
     * {@code Events.totalSessions}, {@code Events.*Successes}, {@code Events.*ConversionRate}.
     * </p>
     * <p>
     * <strong>Invariant</strong>: must return one row per experiment variant so that
     * {@code ExperimentResults.getSessions().getVariants().size() >= 2} holds, preserving the
     * Bayesian calculation gate in {@code ExperimentsAPIImpl}.
     * </p>
     *
     * @param experiment the experiment to query results for
     * @param user       calling user — used by the CubeJS path for client auth; CAEM implementations may ignore
     * @return raw aggregate per-variant analytics result set
     * @throws DotDataException     if the underlying data source cannot be reached or returns an error
     * @throws DotSecurityException if the user lacks access to the analytics backend
     */
    AnalyticsResultSet doExecuteAggregate(Experiment experiment, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Returns per-variant results broken down by day, adjusted for goal type.
     * <p>
     * For {@link Goal.GoalType#MINIMIZE} goals the raw successes and conversion rate from the
     * backend represent the undesired event (e.g. a bounce); this method automatically inverts
     * them so callers always receive semantically correct "successes" regardless of goal type.
     * Replaces {@link ExperimentResultsQueryFactory#createWithDayGranularity(Experiment)}.
     * Used by {@code ExperimentsAPIImpl.getSummary()}.
     * </p>
     *
     * @param experiment the experiment to query results for
     * @param user       calling user
     * @return goal-adjusted per-day per-variant analytics result set
     * @throws DotDataException     if the underlying data source cannot be reached or returns an error
     * @throws DotSecurityException if the user lacks access to the analytics backend
     */
    default AnalyticsResultSet executeByDay(final Experiment experiment, final User user)
            throws DotDataException, DotSecurityException {
        return adjustForGoalType(doExecuteByDay(experiment, user), experiment);
    }

    /**
     * Returns aggregate (non-day) per-variant totals, adjusted for goal type.
     * <p>
     * For {@link Goal.GoalType#MINIMIZE} goals the raw successes and conversion rate from the
     * backend represent the undesired event (e.g. a bounce); this method automatically inverts
     * them so callers always receive semantically correct "successes" regardless of goal type.
     * Replaces {@link ExperimentResultsQueryFactory#create(Experiment)}.
     * Used by {@code ExperimentsAPIImpl.getTotalSessions()}.
     * </p>
     *
     * @param experiment the experiment to query results for
     * @param user       calling user
     * @return goal-adjusted aggregate per-variant analytics result set
     * @throws DotDataException     if the underlying data source cannot be reached or returns an error
     * @throws DotSecurityException if the user lacks access to the analytics backend
     */
    default AnalyticsResultSet executeAggregate(final Experiment experiment, final User user)
            throws DotDataException, DotSecurityException {
        return adjustForGoalType(doExecuteAggregate(experiment, user), experiment);
    }

    private static AnalyticsResultSet adjustForGoalType(final AnalyticsResultSet raw,
                                                         final Experiment experiment) throws DotDataException {
        return isMinimize(experiment) ? invert(raw) : raw;
    }

    private static boolean isMinimize(final Experiment experiment) {
        return experiment.goals()
                .map(goals -> goals.primary().type() == Goal.GoalType.MINIMIZE)
                .orElse(false);
    }

    /**
     * Inverts the {@code *Successes} and {@code *ConversionRate} fields of every row so that
     * a raw "failure count" from the backend becomes a meaningful success count:
     * <ul>
     *   <li>{@code successes = totalSessions - rawSuccesses}</li>
     *   <li>{@code conversionRate = 1.0 - rawConversionRate}</li>
     * </ul>
     */
    private static AnalyticsResultSet invert(final AnalyticsResultSet raw) throws DotDataException {
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final ResultSetItem item : raw) {
            final Map<String, Object> original = item.getAll();
            final Map<String, Object> row = new HashMap<>(original);

            final Object totalSessionsRaw = original.get("Events.totalSessions");
            if (totalSessionsRaw == null) {
                throw new DotDataException(
                        "Analytics response row is missing required field 'Events.totalSessions'; "
                        + "cannot invert MINIMIZE goal results. Row keys: " + original.keySet());
            }
            final long totalSessions = Long.parseLong(totalSessionsRaw.toString());

            original.keySet().stream()
                    .filter(k -> k.endsWith("Successes"))
                    .findFirst()
                    .ifPresent(successKey -> {
                        final long successes = Long.parseLong(original.get(successKey).toString());
                        row.put(successKey, totalSessions - successes);
                    });

            original.keySet().stream()
                    .filter(k -> k.endsWith("ConversionRate"))
                    .findFirst()
                    .ifPresent(rateKey -> {
                        final double rate = Double.parseDouble(original.get(rateKey).toString());
                        // CAEM returns rates as percentages (0–100); invert on the same scale
                        row.put(rateKey, 100.0 - rate);
                    });

            rows.add(row);
        }
        return new AnalyticsResultSetImpl(rows);
    }

}