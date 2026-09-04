package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

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
 *       {@link com.dotcms.analytics.metrics.QueryParameter#getName()} on the goal condition</li>
 *   <li>{@code paramValue} — the query parameter value, sourced from
 *       {@link com.dotcms.analytics.metrics.QueryParameter#getValue()} on the goal condition</li>
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

    private final CaemHttpClient caemHttpClient;

    public UrlParameterCAEMResultQuery(final CaemHttpClient caemHttpClient) {
        this.caemHttpClient = caemHttpClient;
    }

    @Override
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        throw new UnsupportedOperationException("UrlParameterCAEMResultQuery not yet implemented");
    }

    @Override
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        throw new UnsupportedOperationException("UrlParameterCAEMResultQuery not yet implemented");
    }

}
