package com.dotcms.experiments.business.result;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter.Order;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.Experiment;
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
 * @see MetricExperimentResultQuery
 */
public enum ExperimentResultQueryFactory {

    INSTANCE;

    final static Lazy<Map<MetricType, MetricExperimentResultQuery>> experimentResultQueryHelpers =
            Lazy.of(() -> createHelpersMap());


    private static Map<MetricType, MetricExperimentResultQuery> createHelpersMap() {
        return map(
            MetricType.REACH_PAGE, new ReachPageExperimentResultQuery()
        );
    }

    private static CubeJSQuery createRootQuery(final Experiment experiment) {
        return new CubeJSQuery.Builder()
                .dimensions("Events.experiment",
                        "Events.variant",
                        "Events.utcTime",
                        "Events.referer",
                        "Events.url",
                        "Events.lookBackWindow",
                        "Events.eventType"
                )
                .order("Events.lookBackWindow", Order.ASC)
                .order("Events.utcTime", Order.ASC)
                .filter("Events.experiment", Operator.EQUALS, experiment.getIdentifier())
                .build();
    }

    /**
     * Create a {@link CubeJSQuery} according a {@link Experiment} creating the dynamic and static
     * part of the Query.
     *
     * @param experiment {@link Experiment} to use to create the {@link CubeJSQuery}
     * @return The {@link CubeJSQuery} generated
     *
     * @see {@link ExperimentResultQueryFactory}
     */
    public CubeJSQuery create(final Experiment experiment){
        final CubeJSQuery cubeJSQuery = getMetricCubeJSQuery(experiment);
        final CubeJSQuery rootCubeJSQuery = createRootQuery(experiment);
        return CubeJSQuery.Builder.merge(cubeJSQuery, rootCubeJSQuery);
    }

    private static CubeJSQuery getMetricCubeJSQuery(final Experiment experiment) {
        DotPreconditions.notNull(experiment.goals(), "The must have a Goal");
        DotPreconditions.notNull(experiment.goals().orElseThrow(), "The must have a Goal");
        DotPreconditions.notNull(experiment.goals().orElseThrow().primary(), "The must have a Goal");

        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("The Experiment must have a Goal"));

        final Goal primaryGoal = goals.primary();
        final MetricExperimentResultQuery metricExperimentResultQuery = experimentResultQueryHelpers.get()
                .get(primaryGoal.getMetric().type());
        return metricExperimentResultQuery.getCubeJSQuery(experiment);
    }
}
