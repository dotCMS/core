

package com.dotcms.rest.api.v1.experiments;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.ExperimentResults.GoalResult;
import com.dotcms.experiments.business.result.ExperimentResults.VariantResult;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.experiments.ResponseEntityResultExperimentView.ExperimentResultView;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections.map.HashedMap;

/**
 * {@link ResponseEntityView} for an array of {@link ExperimentResultView}
 */
public class ResponseEntityResultExperimentView extends ResponseEntityView<ExperimentResultView> {

    public ResponseEntityResultExperimentView(final ExperimentResults experimentResults) {
        super(new ExperimentResultView(experimentResults));
    }

    public static class ExperimentResultView {

        private long totalSession;
        private Map<String, GoalResultView> goals = new HashMap<>();

        public ExperimentResultView(final ExperimentResults experimentResults) {
            this.totalSession = experimentResults.getTotalSessions();

            goals.put("primary",
                    new GoalResultView(experimentResults.getGoalResults().get(0), totalSession));
        }

        public long getTotalSession() {
            return totalSession;
        }

        public Map<String, GoalResultView> getGoals() {
            return goals;
        }
    }

    private static class GoalResultView {

        private final MetricType metric;
        private final Map<String, VariantResultView> variants = new HashedMap();

        public GoalResultView(final GoalResult goalResult, final long totalSessions) {
            metric = goalResult.getGoal().type();

            for (final Entry<String, VariantResult> entry : goalResult.getVariants().entrySet()) {
                variants.put(entry.getKey(),
                        new VariantResultView(entry.getValue(), totalSessions));
            }
        }

        public MetricType getMetric() {
            return metric;
        }

        public Map<String, VariantResultView> getVariants() {
            return variants;
        }
    }

    private static class VariantResultView {

        final long multiBySession;
        final ShortResult uniqueBySession;

        public VariantResultView(final VariantResult variantResult, final long totalSessions) {
            this.uniqueBySession = new ShortResult(variantResult.totalUniqueBySession(),
                    totalSessions);
            this.multiBySession = variantResult.totalMultiBySession();
        }

        public long getMultiBySession() {
            return multiBySession;
        }

        public ShortResult getUniqueBySession() {
            return uniqueBySession;
        }
    }

    private static class ShortResult {

        private final long count;
        private final float percentage;

        public ShortResult(final int count, final long total) {
            this.count = count;
            this.percentage = total > 0 ? (float) (count * 100) / total : 0;
        }

        public long getCount() {
            return count;
        }

        public float getPercentage() {
            return percentage;
        }
    }
}

