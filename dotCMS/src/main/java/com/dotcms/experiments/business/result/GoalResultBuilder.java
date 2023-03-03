package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.business.result.ExperimentResults.TotalSession;
import com.dotcms.experiments.model.ExperimentVariant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder of {@link GoalResult}
 */
public class GoalResultBuilder {
    final Metric goal;
    final Map<String, VariantResultBuilder> variants;
    public GoalResultBuilder(final Metric goal, final Collection<ExperimentVariant> experimentVariants) {
        this.goal = goal;
        this.variants = experimentVariants.stream()
                .collect(Collectors.toMap(ExperimentVariant::id, VariantResultBuilder::new));
    }

    public void success(final String lookBackWindow, final Event event) {
        final String variantName = event.getVariant()
                .orElseThrow(() -> new IllegalArgumentException("Attribute variant does not exists"));

        final VariantResultBuilder variantResultBuilder = variants.get(variantName);

        if (variantResultBuilder == null) {
            throw new IllegalArgumentException(String.format("Variant %s does not exists in the Experiment", variantName));
        }

        variantResultBuilder.success(lookBackWindow, event);
    }

    public GoalResult build(final TotalSession totalSessions) {
        final List<VariantResult> variantResults = variants.values().stream()
                .map(variantResultBuilder -> {
                    final String variantId = variantResultBuilder.experimentVariant.id();
                    variantResultBuilder.setTotalSession(totalSessions.getTotal());
                    variantResultBuilder.setTotalSessionToVariant(
                            totalSessions.getVariants().get(variantId));
                    return variantResultBuilder;
                })
                .map(variantResultBuilder -> variantResultBuilder.build())
                .collect(Collectors.toList());

        final Map<String, VariantResult> variantResultMap = variantResults.stream()
                .collect(Collectors.toMap(VariantResult::getVariantName,
                        variantResult -> variantResult));

        return new GoalResult(goal, variantResultMap);
    }
}
