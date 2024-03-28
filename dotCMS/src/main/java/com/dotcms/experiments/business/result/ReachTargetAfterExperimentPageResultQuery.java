package com.dotcms.experiments.business.result;

import static com.dotcms.analytics.metrics.EventType.PAGE_VIEW;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.experiments.model.Experiment;

/**
 * Use to create the dynamic part of a {@link Experiment}'s {@link CubeJSQuery} when the {@link Experiment}
 * has a PAGE_REACH goal.
 *
 * The syntax of this part of the {@link CubeJSQuery} is:
 *
 * <code>
 *     {
 *         "dimensions": [
 *              ""Events.referer""
 *         ],
 *         "filters": [
 *              {
 *                  "member": "Events.eventType",
 *                  "operator": "equals",
 *                  "values": ["pageview"]
 *              }
 *         ]
 *     }
 * </code>
 *
 * This Query is merged with the static part create in {@link ExperimentResultsQueryFactory}
 *
 * @see ExperimentResultsQueryFactory
 * @see MetricExperimentResultsQuery
 */
public class ReachTargetAfterExperimentPageResultQuery implements MetricExperimentResultsQuery {

    @Override
    public CubeJSQuery getCubeJSQuery(final Experiment experiment) {

        return new CubeJSQuery.Builder()
                .measures("Events.totalSessions", "Events.targetVisitedAfterSuccesses", "Events.targetVisitedAfterConvertionRate")
                .build();
    }
}
