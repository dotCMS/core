package com.dotcms.experiments.business.result;

import static com.dotcms.analytics.metrics.EventType.PAGE_VIEW;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.experiments.model.Experiment;

/**
 * Use to create the dynamic part of a {@link Experiment}'s {@link CubeJSQuery} when the {@link Experiment}
 * has a BOUNCE_RATE goal.
 *
 * The syntax of this part of the {@link CubeJSQuery} is:
 *
 * <code>
 *     {
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
 * This Query is merged with the static part create in {@link ExperimentResultQueryFactory}
 *
 * @see ExperimentResultQueryFactory
 * @see MetricExperimentResultQuery
 */
public class BounceRateExperimentResultQuery implements  MetricExperimentResultQuery {

    @Override
    public CubeJSQuery getCubeJSQuery(final Experiment experiment) {

        return new CubeJSQuery.Builder()
                .filter("Events.eventType", Operator.EQUALS, PAGE_VIEW.getName())
                .build();
    }

}
