package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.QueryParameter;
import com.dotcms.experiments.model.Experiment;

import java.util.Optional;

/**
 * Utility methods for extracting goal condition values from an {@link Experiment}.
 * Used by CAEM result query classes to build the CAEM API request parameters.
 */
class GoalConditionUtil {

    private GoalConditionUtil() {}

    /**
     * Finds the {@code String} value of the first condition matching the given parameter name
     * in the experiment's primary goal metric conditions.
     *
     * @param experiment    the experiment whose primary goal conditions to search
     * @param parameterName the condition parameter name (e.g. {@code "url"}, {@code "referer"})
     * @return the condition value as a String, or empty if not found
     */
    static Optional<String> findConditionValue(final Experiment experiment,
                                               final String parameterName) {
        return experiment.goals()
                .map(goals -> goals.primary().getMetric().conditions())
                .flatMap(conditions -> conditions.stream()
                        .filter(c -> parameterName.equals(c.parameter()))
                        .map(c -> c.value().toString())
                        .findFirst());
    }

    /**
     * Finds the {@link QueryParameter} value from the experiment's primary goal metric conditions.
     * Used by {@link UrlParameterCAEMResultQuery} to extract {@code paramName} and {@code paramValue}.
     *
     * @param experiment the experiment whose primary goal conditions to search
     * @return the {@link QueryParameter}, or empty if not found
     */
    static Optional<QueryParameter> findQueryParameter(final Experiment experiment) {
        return experiment.goals()
                .map(goals -> goals.primary().getMetric().conditions())
                .flatMap(conditions -> conditions.stream()
                        .filter(c -> "queryParameter".equals(c.parameter()))
                        .map(c -> (QueryParameter) c.value())
                        .findFirst());
    }

}
