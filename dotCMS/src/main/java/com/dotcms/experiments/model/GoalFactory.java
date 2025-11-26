package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.experiments.model.Goal.GoalType;

import java.util.Map;
import java.util.function.Function;

/**
 * Factory to create a {@link Goal} from the {@link Metric} that this Goal is going to use
 *
 * @see ReachPageGoal
 * @see BounceRateGoal
 */
public class GoalFactory {

    private static final Map<MetricType, Function<Metric, Goal>> builder = Map.of(
            MetricType.EXIT_RATE,  metric -> new Goal(metric, GoalType.MINIMIZE),
            MetricType.BOUNCE_RATE,  metric -> new Goal(metric, GoalType.MINIMIZE),
            MetricType.REACH_PAGE, metric -> new Goal(metric, GoalType.MAXIMIZE),
            MetricType.CLICK_ON_ELEMENT, metric -> new Goal(metric, GoalType.MAXIMIZE),
            MetricType.URL_PARAMETER, metric -> new Goal(metric, GoalType.MAXIMIZE)
    );

    /**
     * Create a Goal
     *
     * @param metric
     * @return
     */
    public static Goal create(final Metric metric) {
        final MetricType type = metric.type();
        return builder.get(type).apply(metric);
    }
}
