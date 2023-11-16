package com.dotcms.experiments.business.result;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.experiments.model.Experiment;

/**
 * Util class to create the CubeJS Query for a Exit Rate Goal, this class just create the part of the Query that is
 * different for a Exit rate Goal later this Query is merged with a Root query  that is the same for all the Goals.
 *
 * The specific CubeJS Query for a Bounce Rate goal is:
 *
 * <code>
 * {
 *     measures: ["Events.totalSessions", "Events.exitRateSuccesses", "Events.exitRateConvertionRate"]
 * }
 * </code>
 *
 * @see ExperimentResultsQueryFactory#create(Experiment)
 */
public class ExitRateResultQuery implements MetricExperimentResultsQuery {

    @Override
    public CubeJSQuery getCubeJSQuery(final Experiment experiment) {

        return new CubeJSQuery.Builder()
                .measures("Events.totalSessions", "Events.exitRateSuccesses", "Events.exitRateConvertionRate")
                .build();
    }
}
