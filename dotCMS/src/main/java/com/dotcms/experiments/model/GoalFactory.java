package com.dotcms.experiments.model;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory to create a {@link Goal} from the {@link Metric} that this Goal is going to use
 *
 * @see ReachPageGoal
 * @see BounceRateGoal
 */
public class GoalFactory {

    private static final Map<MetricType, Function<Metric, Goal>> builder = map(
            MetricType.BOUNCE_RATE,  metric -> new BounceRateGoal(metric),
            MetricType.REACH_PAGE, metric -> new ReachPageGoal(metric)
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
