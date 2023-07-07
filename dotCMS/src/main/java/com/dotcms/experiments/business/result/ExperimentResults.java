package com.dotcms.experiments.business.result;

import com.dotcms.analytics.bayesian.model.BayesianResult;

import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.util.DotPreconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * Represent a partial or total {@link ExperimentResults}.
 * The ExperimentResult include:
 * <ul>
 *     <li>How many Session the {@link com.dotcms.experiments.model.Experiment}'s Page was view.</li>
 *     <li>How many Sessions the {@link com.dotcms.experiments.model.Goals} was success</li>
 *     <li>How many times the {@link com.dotcms.experiments.model.Goals} was success no matter
 *     if it was success several times in the sae session</li>
 *     <li>Bayesian results when available {@link BayesianResult}</li>
 * </ul>
 *
 */
public class ExperimentResults {

    private final TotalSession sessions;
    private final Map<String, GoalResults> goals;
    private BayesianResult bayesianResult;

    private ExperimentResults(final TotalSession sessions,
                              final Map<String, GoalResults> goalResults) {
        this.sessions = sessions;
        this.goals = goalResults;
    }

    /**
     * Return How many Session the {@link com.dotcms.experiments.model.Experiment}'s Page was view,
     * put in another way how many Session go into the Experiment.
     *
     * @return
     */
    public TotalSession getSessions() {
        return sessions;
    }

    /**
     * Return the result split by {@link com.dotcms.variant.model.Variant}.
     *
     * @return
     */
    public Map<String, GoalResults> getGoals() {
        return goals;
    }

    public BayesianResult getBayesianResult() {
        return bayesianResult;
    }

    public void setBayesianResult(BayesianResult bayesianResult) {
        this.bayesianResult = bayesianResult;
    }

    public static class Builder {

        private final TotalSessionBuilder totalSessionsBuilder;
        private final Map<String, GoalResultsBuilder> goals = new HashMap<>();
        private final Collection<ExperimentVariant> variants;
        private TrafficProportion trafficProportion;

        public Builder(final Collection<ExperimentVariant> variants){
            this.variants = variants;
            totalSessionsBuilder = new TotalSessionBuilder(variants);
        }

        public Builder addPrimaryGoal(final Goal goal) {
            this.goals.put("primary", new GoalResultsBuilder(goal, variants));
            return this;
        }

        public ExperimentResults build() {
            final TotalSession totalSessions = totalSessionsBuilder.build();

            final Map<String, GoalResults> goalResultMap = goals.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey,
                            entry -> entry.getValue().build(totalSessions, trafficProportion)));

            return new ExperimentResults(totalSessions, goalResultMap);
        }

        public GoalResultsBuilder goal(final Goal goal) {
            return this.goals.values().stream()
                    .filter(builder -> builder.goal.name().equals(goal.name()))
                    .limit(1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Goal does not exists"));
        }

        public void addSession(final BrowserSession browserSession) {
            final String variantName = browserSession.getVariant().orElseThrow();
            totalSessionsBuilder.count(variantName);
        }

        public void trafficProportion(final TrafficProportion trafficProportion) {
            this.trafficProportion = trafficProportion;
        }

        private static class TotalSessionBuilder {

            private final Map<String, Long> totalSessions = new HashMap<>();
            private final List<String> variantsName;

            public TotalSessionBuilder(Collection<ExperimentVariant> variantsName) {
                this.variantsName = variantsName.stream()
                        .map(experimentVariant -> experimentVariant.id())
                        .collect(Collectors.toList());
            }

            public void count(final String variantName) {
                DotPreconditions.isTrue(variantsName.contains(variantName), "Variant does not exists");

                final Long currentTotalSessionsVariant = totalSessions.getOrDefault(variantName, 0l) + 1;
                totalSessions.put(variantName, currentTotalSessionsVariant);
            }

            private long getTotal() {
                return totalSessions.values().stream().mapToLong(Long::longValue).sum();
            }

            public TotalSession build(){
                final Map<String, Long> sessionsResult = new HashMap<>();

                for (String variantName : variantsName) {
                    final Long count = totalSessions.get(variantName);
                    sessionsResult.put(variantName, count != null ? count : 0);
                }

                return new TotalSession(getTotal(), sessionsResult);
            }
        }

    }

    public static class TotalSession {

        private final long total;
        private final Map<String, Long> variants;

        public TotalSession(final long total, final Map<String, Long> variants) {
            this.total = total;
            this.variants = variants;
        }

        public long getTotal() {
            return total;
        }

        public Map<String, Long> getVariants() {
            return variants;
        }

    }

}
