package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.QueryParameter;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

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
     * Resolves the URI of the experiment page from {@link Experiment#pageId()}.
     * Used as the {@code referencePage} parameter in CAEM queries that need to scope
     * results to the page the experiment is running on.
     *
     * @param experiment the experiment whose page URI to resolve
     * @return the page URI (e.g. {@code /about-us})
     * @throws DotDataException if the page cannot be found or loaded
     */
    static String resolvePageUri(final Experiment experiment) throws DotDataException {
        final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(
                APILocator.getContentletAPI()
                        .findContentletByIdentifierAnyLanguage(experiment.pageId(), false));
        return page.getURI();
    }

    /**
     * Finds the {@link Operator} of the first condition matching the given parameter name
     * in the experiment's primary goal metric conditions.
     * Used to pass {@code targetMatchType} to CAEM for URL-matching goals.
     *
     * @param experiment    the experiment whose primary goal conditions to search
     * @param parameterName the condition parameter name (e.g. {@code "url"})
     * @return the operator (e.g. {@link Operator#EQUALS}, {@link Operator#CONTAINS}), or empty if not found
     */
    static Optional<Operator> findConditionOperator(final Experiment experiment,
                                                    final String parameterName) {
        return experiment.goals()
                .map(goals -> goals.primary().getMetric().conditions())
                .flatMap(conditions -> conditions.stream()
                        .filter(c -> parameterName.equals(c.parameter()))
                        .map(c -> c.operator())
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
