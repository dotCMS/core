package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.experiments.model.ExperimentVariant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represent a partial or total Result for a specific Goal into a {@link com.dotcms.experiments.model.Experiment}.
 * The GoalResult include the follow data for all the variants:
 *
 * <ul>
 *     <li>How many Sessions the {@link com.dotcms.experiments.model.Experiment}'s Goal was success</li>
 *     <li>How many times the {@link com.dotcms.experiments.model.Experiment}'s Goal was success no matter
 *     if it was success several times in the sae session</li>
 * </ul>
 *
 */
public class GoalResult {

    private Metric goal;
    final Map<String, VariantResult> variants;
    public GoalResult(final Metric metric, final Map<String, VariantResult> variants) {
        this.goal = metric;
        this.variants = variants;
    }

    public Metric getGoal() {
        return goal;
    }

    public Map<String, VariantResult> getVariants() {
        return variants;
    }
}