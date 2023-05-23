package com.dotcms.experiments.business.result;

import com.dotcms.experiments.model.Goal;
import java.util.Map;

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
}