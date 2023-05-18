package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.business.result.ExperimentResults.TotalSession;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.TrafficProportion;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder of {@link GoalResults}
 */
public class GoalResultsBuilder {
    final Metric goal;
    final Map<String, VariantResultsBuilder> variants;
    public GoalResultsBuilder(final Metric goal, final Collection<ExperimentVariant> experimentVariants) {
        this.goal = goal;
        this.variants = experimentVariants.stream()
                .collect(Collectors.toMap(ExperimentVariant::id, VariantResultsBuilder::new));
    }

    public VariantResultsBuilder variant(final String variantId) {
        return variants.get(variantId);
    }
    public void success(final String lookBackWindow, final Event event) {
        final String variantName = event.getVariant()
                .orElseThrow(() -> new IllegalArgumentException("Attribute variant does not exists"));

        final VariantResultsBuilder variantResultsBuilder = variants.get(variantName);

        if (variantResultsBuilder == null) {
            throw new IllegalArgumentException(String.format("Variant %s does not exists in the Experiment", variantName));
        }

        variantResultsBuilder.success(lookBackWindow, event);
    }

    public GoalResults build(final TotalSession totalSessions, final TrafficProportion trafficProportion) {

        final Map<String, Float> trafficProportionMap = trafficProportion.variants().stream()
                .collect(Collectors.toMap(ExperimentVariant::id, ExperimentVariant::weight));

        final List<Instant> allDates = variants.values().stream()
                .map(variantResultsBuilder -> variantResultsBuilder.getEventDates())
                .flatMap(variantDates -> variantDates.stream())
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        final List<VariantResults> variantResults = variants.values().stream()
                .map(variantResultsBuilder -> {
                    final String variantId = variantResultsBuilder.experimentVariant.id();
                    variantResultsBuilder.setTotalSession(totalSessions.getTotal());
                    variantResultsBuilder.setTotalSessionToVariant(
                            totalSessions.getVariants().get(variantId));
                    variantResultsBuilder.weight(trafficProportionMap.get(variantId));
                    return variantResultsBuilder;
                })
                .map(variantResultsBuilder -> variantResultsBuilder.build(allDates))
                .collect(Collectors.toList());

        final Map<String, VariantResults> variantResultMap = variantResults.stream()
                .collect(Collectors.toMap(VariantResults::getVariantName,
                        variantResult -> variantResult));

        return new GoalResults(goal, variantResultMap);
    }
}
