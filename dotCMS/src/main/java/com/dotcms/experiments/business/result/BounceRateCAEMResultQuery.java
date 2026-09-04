package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * CAEM-backed implementation of {@link ExperimentGoalResultsQuery} for {@code BOUNCE_RATE} goals.
 * Queries {@code GET /v1/analytics/sessions} with bounce metrics and {@code variant} dimension.
 */
public class BounceRateCAEMResultQuery implements ExperimentGoalResultsQuery {

    private final CaemHttpClient caemHttpClient;

    public BounceRateCAEMResultQuery(final CaemHttpClient caemHttpClient) {
        this.caemHttpClient = caemHttpClient;
    }

    @Override
    public AnalyticsResultSet executeByDay(final Experiment experiment,
                                           final User user) throws DotDataException, DotSecurityException {
        throw new UnsupportedOperationException("BounceRateCAEMResultQuery not yet implemented");
    }

    @Override
    public AnalyticsResultSet executeAggregate(final Experiment experiment,
                                               final User user) throws DotDataException, DotSecurityException {
        throw new UnsupportedOperationException("BounceRateCAEMResultQuery not yet implemented");
    }

}
