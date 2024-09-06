package com.dotcms.experiments.business.result;

import com.dotcms.analytics.bayesian.model.BayesianResult;

import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.util.UtilMethods;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
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

        private final Map<String, Long> totalSessions = new HashMap<>();
        private final Map<String, GoalResults.Builder> goals = new HashMap<>();
        private final Collection<ExperimentVariant> variants;
        private TrafficProportion trafficProportion;

        public Builder(final Collection<ExperimentVariant> variants){
            this.variants = variants;
        }

        public Builder addPrimaryGoal(final Goal goal) {
            this.goals.put("primary", new GoalResults.Builder(goal, variants));
            return this;
        }

        public ExperimentResults build() {
            final TotalSession totalSessions = new TotalSession(this.totalSessions);

            final Map<String, GoalResults> goalResultMap = goals.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey,
                            entry -> entry.getValue().build()));

            return new ExperimentResults(totalSessions, goalResultMap);
        }

        public GoalResults.Builder goal(final Goal goal) {
            return this.goals.values().stream()
                    .filter(builder -> builder.goal.name().equals(goal.name()))
                    .limit(1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Goal does not exists"));
        }

        public void addTotalSession(final String variantName, final long total) {
            totalSessions.put(variantName, total);
        }

        public void trafficProportion(final TrafficProportion trafficProportion) {
            this.trafficProportion = trafficProportion;
        }

    }

    public static class TotalSession {

        private final long total;
        private final Map<String, Long> totalSessionByVariant;

        public TotalSession(final Map<String, Long> totalSessionByVariant) {
            this.total = totalSessionByVariant.values().stream().reduce(0l, Long::sum);
            this.totalSessionByVariant = totalSessionByVariant;
        }

        public long getTotal() {
            return total;
        }

        public Map<String, Long> getVariants() {
            return totalSessionByVariant;
        }

    }

}
