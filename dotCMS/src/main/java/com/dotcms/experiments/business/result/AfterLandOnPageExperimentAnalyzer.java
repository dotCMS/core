package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;

import com.dotcms.experiments.model.Experiment;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.stream.Collectors;

/**
 * Analyze  a set of {@link BrowserSession} to figure out if a set of Condition was meet after a specific Page
 * was visited.
 *
 */
public class AfterLandOnPageExperimentAnalyzer implements MetricExperimentAnalyzer {


    /**
     *
     * @param metric
     * @param browserSession
     * @return
     */
    @Override
    public Collection<Event> getOccurrences(final Metric metric, final BrowserSession browserSession) {

        final Collection<Event> results = new ArrayList<>();

        final String visitBeforeUrl = metric.conditions().stream()
                .filter(condition -> condition.parameter().equals("visitBefore"))
                .map(condition -> condition.value())
                .limit(1)
                .findFirst()
                .orElse(StringPool.BLANK);

        final List<Event> events = getEventAfterVisitUrl(visitBeforeUrl, browserSession.getEvents())
                .stream()
                .filter(event -> event.getType() == EventType.PAGE_VIEW)
                .collect(Collectors.toList());

        for (final Event event : events) {
            if (metric.validateConditions(event)) {
                results.add(event);
            }
        }

        return results;

    }

    private List<Event> getEventAfterVisitUrl(final String visitBeforeUrl, final List<Event> events) {
        List<Event> afterCondition = new ArrayList<>();

        boolean foundCondition = false;

        for (final Event event : events) {
            if (!foundCondition) {
                foundCondition = event.getUrl().contains(visitBeforeUrl);
            }

            if (foundCondition) {
                afterCondition.add(event);
            }
        }

        return afterCondition;
    }
}
