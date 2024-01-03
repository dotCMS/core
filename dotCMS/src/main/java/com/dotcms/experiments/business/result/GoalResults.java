package com.dotcms.experiments.business.result;

import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goal;

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
public class GoalResults {

    private Goal goal;
    final Map<String, VariantResults> variants;
    public GoalResults(final Goal goal, final Map<String, VariantResults> variants) {
        this.goal = goal;
        this.variants = variants;
    }

    public Goal getGoal() {
        return goal;
    }

    public Map<String, VariantResults> getVariants() {
        return variants;
    }

    public static class Builder {
        final Goal goal;
        final Map<String, VariantResults.Builder> variants;

        public Builder(final Goal goal, final Collection<ExperimentVariant> experimentVariants) {
            this.goal = goal;
            this.variants = experimentVariants.stream()
                    .collect(Collectors.toMap(ExperimentVariant::id, VariantResults.Builder::new));
        }

        public void add(final String variantName, final String day, final VariantResults.ResultResumeItem resultResumeItem) {
            for (final String innerVariantName : variants.keySet()) {
                if (variantName.equals(innerVariantName)) {
                    variants.get(innerVariantName).add(day, resultResumeItem);
                } else {
                    variants.get(innerVariantName).addIfNotExists(day, new VariantResults.ResultResumeItem(0, 0, 0));
                }
            }
        }

        public GoalResults build(){
            final Map<String, VariantResults> resultsMap = variants.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
            return new GoalResults(goal, resultsMap);
        }

        public void uniqueBySession(final String variantName, final VariantResults.UniqueBySessionResume uniqueBySessionResume) {
            variants.get(variantName).uniqueBySession(uniqueBySessionResume);
        }
    }
}