package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.CubeJSResultSet;

import com.dotcms.experiments.model.Experiment;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collection;
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
     * @param metric
     * @param browserSession
     * @return
     */
    @Override
    public Collection<Event> getOccurrences(final Metric metric, final BrowserSession browserSession) {

        final Collection<Event> results = new ArrayList<>();

        final List<Event> events = browserSession.getEvents().stream()
                .filter(event -> event.getType() == EventType.PAGE_VIEW)
                .collect(Collectors.toList());

        for (final Event event : events) {
            if (metric.validateConditions(event)) {
                results.add(event);
            }
        }

        return results;

    }
}
