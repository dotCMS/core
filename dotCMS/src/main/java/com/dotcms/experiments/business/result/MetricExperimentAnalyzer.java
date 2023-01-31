package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.cube.CubeJSResultSet;
import java.util.List;

public interface MetricExperimentAnalyzer {

    void addResults(final Metric goal, final BrowserSession browserSession, final ExperimentResult.Builder experimentResultBuilder);

}
