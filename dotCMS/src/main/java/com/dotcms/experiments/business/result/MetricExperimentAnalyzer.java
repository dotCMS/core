package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;


public interface MetricExperimentAnalyzer {

    void addResults(final Metric goal, final BrowserSession browserSession, final ExperimentResult.Builder experimentResultBuilder);

}
