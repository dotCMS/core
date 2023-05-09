package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.model.Experiment;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyze  a set of {@link BrowserSession} to get the total or partial {@link Experiment} result
 * when the {@link Experiment} is using a BOUNCE_RATE {@link com.dotcms.experiments.model.Goals}.
 *
 */
public class BounceRateExperimentAnalyzer implements MetricExperimentAnalyzer  {

    /**
     * Iterate through the {@link BrowserSession} and check if the last {@link EventType#PAGE_VIEW}
     * events was in the url set as parameters if it is then it is count as a BOUNCE_RATE.
     *
     * @see com.dotcms.analytics.metrics.MetricType#BOUNCE_RATE
     * @see MetricExperimentAnalyzer
     *
     * @param goal
     * @param browserSession
     * @param experimentResultBuilder
     */
    @Override
    public void addResults(final Metric goal, final BrowserSession browserSession,
            final ExperimentResults.Builder experimentResultBuilder) {
        final List<Event> events = browserSession.getEvents().stream()
                .filter(event -> event.getType() == EventType.PAGE_VIEW)
                .collect(Collectors.toList());

        final Event lastEvent = events.get(events.size() - 1);

        if (goal.validateConditions(lastEvent)) {
            experimentResultBuilder.goal(goal).success(browserSession.getLookBackWindow(), lastEvent);
        }
    }


}
