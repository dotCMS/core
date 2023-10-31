package com.dotcms.experiments.business.result;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.experiments.model.Experiment;

public class ExitRateResultQuery implements MetricExperimentResultsQuery {

    @Override
    public CubeJSQuery getCubeJSQuery(final Experiment experiment) {

        return new CubeJSQuery.Builder()
                .measures("Events.totalSessions", "Events.exitRateSuccesses", "Events.exitRateConvertionRate")
                .build();
    }
}
