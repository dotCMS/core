package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.DotPreconditions;
import io.vavr.Lazy;

import java.util.Map;

/**
 * Factory to create the {@link CubeJSQuery} for a specific Experiment, this {@link CubeJSQuery} is used
 * to get all the events trigger for a specific {@link Experiment} from the CubeJS server.
 *
 * The Query generated has a static part and a dynamic part:
 *
 * - Static part: the format of this part is allways the same for all the {@link com.dotcms.experiments.model.Experiment}
 *
 * <code>
 * {
 +   "dimensions":[
 +       "Events.referer",
 +       "Events.experiment",
 +       "Events.variant",
 +       "Events.utcTime",
 +       "Events.url\",
 +       "Events.lookBackWindow",
 +       "Events.eventType"
 +   ],
 *   "filters": [
 *      {
 *          "member": "Events.experiment",
 *          "operator": "equals",
 *          "values": [experiment_identifier]
 *      },
 *   ],
 *   "order": {
 *       "Events.lookBackWindow': "asc",
 *       "Events.utcTime": "asc"
 *   }
 * }
 * </code>
 *
 * where:
 *
 * experiment_identifier: is the {@link Experiment}'s id.
 *
 * - Dynamic part: this part depends of the {@link com.dotcms.experiments.model.Goals} of the
 * {@link Experiment}.
 *
 * both part are merged using {@link CubeJSQuery.Builder#merge(CubeJSQuery, CubeJSQuery)} method.
 *
 * @see MetricExperimentResultsQuery
 */
public enum ExperimentResultsQueryFactory {

    INSTANCE;

    final static Lazy<Map<MetricType, MetricExperimentResultsQuery>> experimentResultQueryHelpers =
            Lazy.of(() -> createHelpersMap());


    private static Map<MetricType, MetricExperimentResultsQuery> createHelpersMap() {
        return Map.of(
            MetricType.EXIT_RATE, new ExitRateResultQuery(),
            MetricType.REACH_PAGE, new ReachTargetAfterExperimentPageResultQuery(),
            MetricType.BOUNCE_RATE, new BounceRateResultQuery(),
            MetricType.URL_PARAMETER, new ReachTargetAfterExperimentPageResultQuery()
        );
    }

    private static CubeJSQuery createRootQuery(final Experiment experiment, final boolean dayGranularity) {

        DotPreconditions.isTrue(experiment.status() == Status.RUNNING || experiment.status() == Status.ENDED,
                "Experiment must be running or Ended");

        final String runningId = experiment.runningIds().getCurrent().orElseThrow().id();

        final CubeJSQuery.Builder builder = new CubeJSQuery.Builder()
                .dimensions("Events.variant")
                .order("Events.day", Filter.Order.ASC)
                .filter("Events.experiment", Operator.EQUALS, experiment.getIdentifier())
                .filter("Events.runningId", Operator.EQUALS, runningId);

        if (dayGranularity) {
            builder.timeDimension("Events.day", "day");
        }

        return builder.build();
    }

    /**
     * Create a {@link CubeJSQuery} according a {@link Experiment} creating the dynamic and static
     * part of the Query.
     *
     * @param experiment {@link Experiment} to use to create the {@link CubeJSQuery}
     * @return The {@link CubeJSQuery} generated
     *
     * @see {@link ExperimentResultsQueryFactory}
     */
    public CubeJSQuery createWithDayGranularity(final Experiment experiment){
        final CubeJSQuery cubeJSQuery = getMetricCubeJSQuery(experiment);
        final CubeJSQuery rootCubeJSQuery = createRootQuery(experiment, true);
        return CubeJSQuery.Builder.merge(cubeJSQuery, rootCubeJSQuery);
    }

    /**
     * Create the CubeJS Query to get the results for an Experiment, this query has two parts:
     *
     * - The specific Experiment's Goal query: This Query is different for each Goal.
     * - The Root Query: This is the same no matter the Goal of the Experiment, this is the follow:
     *
     * <code>
     *     {
     *         dimensions: ['Events.variant'],
     *         "order": {
     *              "Events.day": "asc",
     *          },
     *          "filters": [
     *              {
     *                  "member": "Events.experiment",
     *                  "operator": "equals",
     *                  "values": ["[experiment_id]"]
     *              },
     *              {
     *                  "member": "Events.runningId",
     *                  "operator": "equals",
     *                  "values": ["[current_experiment_running_id]"]
     *              }
     *          ]
     *     }
     * </code>
     *
     * These two queries are merge to get the Experiment Query.
     *
     * @see BounceRateResultQuery
     * @see ExitRateResultQuery
     * @see ReachTargetAfterExperimentPageResultQuery
     *
     * @param experiment
     * @return
     */
    public CubeJSQuery create(final Experiment experiment){
        final CubeJSQuery cubeJSQuery = getMetricCubeJSQuery(experiment);
        final CubeJSQuery rootCubeJSQuery = createRootQuery(experiment, false);
        return CubeJSQuery.Builder.merge(cubeJSQuery, rootCubeJSQuery);
    }

    private static CubeJSQuery getMetricCubeJSQuery(final Experiment experiment) {
        DotPreconditions.notNull(experiment.goals(), "The must have a Goal");
        DotPreconditions.notNull(experiment.goals().orElseThrow(), "The must have a Goal");
        DotPreconditions.notNull(experiment.goals().orElseThrow().primary(), "The must have a Goal");

        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("The Experiment must have a Goal"));

        final Goal primaryGoal = goals.primary();
        final MetricExperimentResultsQuery metricExperimentResultQuery = experimentResultQueryHelpers.get()
                .get(primaryGoal.getMetric().type());
        return metricExperimentResultQuery.getCubeJSQuery(experiment);
    }
}
