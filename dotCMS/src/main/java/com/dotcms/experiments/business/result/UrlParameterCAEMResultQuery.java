package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.QueryParameter;
import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * CAEM-backed implementation of {@link ExperimentGoalResultsQuery} for {@code URL_PARAMETER} goals.
 *
 * <p>Calls {@code GET /v1/analytics/sessions/behavior} with the following parameters:</p>
 * <ul>
 *   <li>{@code behavior=urlParam} — instructs CAEM to count sessions where at least one
 *       page event contained the target URL query parameter; "any event matches" semantics
 *       are enforced by CAEM, not by dotCMS</li>
 *   <li>{@code experimentId} — the experiment's identifier</li>
 *   <li>{@code runningId} — the current running ID of the experiment</li>
 *   <li>{@code paramName} — the query parameter name, sourced from
 *       {@link QueryParameter#getName()} on the goal condition</li>
 *   <li>{@code paramValue} — the query parameter value, sourced from
 *       {@link QueryParameter#getValue()} on the goal condition</li>
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
 *   <li>{@code successSessions} → {@code Events.reachPageRateSuccesses}</li>
 *   <li>{@code successRate} → {@code Events.reachPageRateConversionRate}</li>
 * </ul>
 *
 * <p>Both methods surface CAEM errors as {@link com.dotmarketing.exception.DotDataException}
 * rather than swallowing them silently (FR-017). An empty CAEM response (zero rows) is
 * returned as an empty {@link AnalyticsResultSet} — not an error (FR-014).</p>
 *
 * @see ExperimentGoalResultsQuery
 * @see ExperimentResultsQueryFactory
 * @see CaemHttpClient
 */
public class UrlParameterCAEMResultQuery implements ExperimentGoalResultsQuery {

    private static final String BEHAVIOR_PATH = "/v1/analytics/sessions/behavior";

    private final CaemHttpClient caemHttpClient;

    public UrlParameterCAEMResultQuery(final CaemHttpClient caemHttpClient) {
        this.caemHttpClient = caemHttpClient;
    }

    @Override
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        return caemHttpClient.get(BEHAVIOR_PATH, buildParams(experiment, "variant,day"), null);
    }

    @Override
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        return caemHttpClient.get(BEHAVIOR_PATH, buildParams(experiment, "variant"), null);
    }

    private static Map<String, String> buildParams(final Experiment experiment,
                                                   final String dimensions) {
        final String runningId = experiment.runningIds().getCurrent().orElseThrow().id();
        final Map<String, String> params = new HashMap<>();
        params.put("experimentId", experiment.getIdentifier());
        params.put("runningId", runningId);
        params.put("behavior", "urlParam");
        params.put("dimensions", dimensions);
        GoalConditionUtil.findQueryParameter(experiment).ifPresent(qp -> {
            params.put("paramName", qp.getName());
            params.put("paramValue", qp.getValue());
        });
        return params;
    }

}
