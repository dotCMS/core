package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.CubeJSResultSet;
import com.dotcms.experiments.model.Experiment;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Analyze  a set of {@link BrowserSession} to get the total or partial {@link Experiment} result
 * when the {@link Experiment} is using a PAGE_REACH {@link com.dotcms.experiments.model.Goals}.
 *
 */
public class ReachPageExperimentAnalyzer implements MetricExperimentAnalyzer {

    /**
     *
     * @param goal
     * @param browserSession
     * @param experimentResultBuilder
     */
    @Override
    public void addResults(final Metric goal, final BrowserSession session,
            final ExperimentResult.Builder experimentResultBuilder) {

        final List<Event> events = session.getEvents().stream()
                .filter(event -> event.getType() == EventType.PAGE_VIEW)
                .collect(Collectors.toList());

        for (final Event event : events) {

            final ImmutableList<Condition> conditions = goal.conditions();
            boolean isValid = true;

            for (final Condition condition : conditions) {

                final String realValue = event.get(condition.parameter())
                        .map(value -> value.toString())
                        .orElse(StringPool.BLANK);

                final String valueToCompare = condition.value();

                isValid = isValid && condition.operator().getFunction()
                        .apply(realValue, valueToCompare);

                if (!isValid) {
                    break;
                }
            }

            if (isValid) {
                experimentResultBuilder.count(goal, session.getLookBackWindow(), event);
            }
        }

    }
}
