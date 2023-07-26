package com.dotcms.experiments.business.result;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.experiments.model.Experiment;

/**
 * Use to create the dynamic part of a {@link Experiment}'s {@link CubeJSQuery}.
 *
 * @see ExperimentResultsQueryFactory
 */
public interface MetricExperimentResultsQuery {

    /**
     * Return the dynamic part of the {@link CubeJSQuery} for an {@link Experiment}
     * @param experiment
     * @return
     */
    CubeJSQuery getCubeJSQuery(final Experiment experiment);

}
