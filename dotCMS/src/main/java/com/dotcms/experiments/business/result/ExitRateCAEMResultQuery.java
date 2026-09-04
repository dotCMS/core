package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * CAEM-backed implementation of {@link ExperimentGoalResultsQuery} for {@code EXIT_RATE} goals.
 *
 * <p>Calls {@code GET /v1/analytics/sessions} with the following parameters:</p>
 * <ul>
 *   <li>{@code metrics=totalSessions,exitSessions,exitRate}</li>
 *   <li>{@code experimentId} — the experiment's identifier</li>
 *   <li>{@code runningId} — the current running ID of the experiment</li>
 *   <li>{@code referencePage} — the reference page configured on the experiment's exit rate goal
 *       condition; sessions that did not exit on this page are excluded by CAEM</li>
 *   <li>{@code dimensions=variant} for {@link #executeAggregate} (aggregate per-variant totals)</li>
 *   <li>{@code dimensions=variant,day} for {@link #executeByDay} (per-day per-variant breakdown)</li>
 * </ul>
 *
 * <p>The CAEM response fields are mapped to the {@code Events.*} naming convention expected by the
 * processing loops in {@code ExperimentsAPIImpl}:</p>
 * <ul>
 *   <li>{@code variant} → {@code Events.variant}</li>
 *   <li>{@code day} → {@code Events.day} (executeByDay only)</li>
 *   <li>{@code totalSessions} → {@code Events.totalSessions}</li>
 *   <li>{@code exitSessions} → {@code Events.exitRateSuccesses}</li>
 *   <li>{@code exitRate} → {@code Events.exitRateConversionRate}</li>
 * </ul>
 *
 * <p>Both methods surface CAEM errors as {@link com.dotmarketing.exception.DotDataException}
 * rather than swallowing them silently (FR-017). An empty CAEM response (zero rows — e.g. no
 * sessions exited on the configured reference page) is returned as an empty
 * {@link AnalyticsResultSet} — not an error (FR-014).</p>
 *
 * @see ExperimentGoalResultsQuery
 * @see ExperimentResultsQueryFactory
 * @see CaemHttpClient
 */
public class ExitRateCAEMResultQuery implements ExperimentGoalResultsQuery {

    private static final String SESSIONS_PATH = "/v1/analytics/sessions";
    private static final String METRICS = "totalSessions,exitSessions,exitRate";

    private final CaemHttpClient caemHttpClient;

    public ExitRateCAEMResultQuery(final CaemHttpClient caemHttpClient) {
        this.caemHttpClient = caemHttpClient;
    }

    @Override
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        return caemHttpClient.get(SESSIONS_PATH, buildParams(experiment, "variant,day"), null);
    }

    @Override
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        return caemHttpClient.get(SESSIONS_PATH, buildParams(experiment, "variant"), null);
    }

    private static Map<String, String> buildParams(final Experiment experiment,
                                                   final String dimensions) {
        final String runningId = experiment.runningIds().getCurrent().orElseThrow().id();
        final Map<String, String> params = new HashMap<>();
        params.put("experimentId", experiment.getIdentifier());
        params.put("runningId", runningId);
        params.put("metrics", METRICS);
        params.put("dimensions", dimensions);
        GoalConditionUtil.findConditionValue(experiment, "url")
                .ifPresent(ref -> params.put("referencePage", ref));
        return params;
    }

}
