package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;

import com.dotcms.experiments.model.Experiment;

/**
 * Analyze  a set of {@link BrowserSession} to get the total or partial {@link Experiment} results
 * we need to create a concrete class for each {@link com.dotcms.analytics.metrics.MetricType} that
 * we need to support.
 *
 */
public interface MetricExperimentAnalyzer {

    void addResults(final Metric goal, final BrowserSession browserSession, final ExperimentResults.Builder experimentResultBuilder);

}
