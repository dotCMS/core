package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;

import com.dotcms.experiments.model.Experiment;
import java.util.Collection;

/**
 * Analyze  a set of {@link BrowserSession} to get the total or partial {@link Experiment} results
 * we need to create a concrete class for each {@link com.dotcms.analytics.metrics.MetricType} that
 * we need to support.
 *
 */
public interface MetricExperimentAnalyzer {

    /**
     * Look for all the occurrences of a Metric inside a {@link BrowserSession}
     *
     * @param metric
     * @param browserSession
     * @return Set of {@link Event} where the Metric was met.
     */
    Collection<Event> getOccurrences(final Metric metric, final BrowserSession browserSession);

}
