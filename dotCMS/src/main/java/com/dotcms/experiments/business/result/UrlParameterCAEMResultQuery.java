package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * CAEM-backed implementation of {@link ExperimentGoalResultsQuery} for {@code URL_PARAMETER} goals.
 * Queries {@code GET /v1/analytics/sessions/behavior} with {@code behavior=urlParam},
 * {@code paramName}, and {@code paramValue} from the goal's {@code QueryParameter} condition.
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
