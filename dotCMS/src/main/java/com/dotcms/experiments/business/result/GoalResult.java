package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.experiments.model.ExperimentVariant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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