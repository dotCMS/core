package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Represent a partial or total {@link ExperimentResults}.
 * The ExperimentResult include:
 * <ul>
 *     <li>How many Session the {@link com.dotcms.experiments.model.Experiment}'s Page was view.</li>
 *     <li>How many Sessions the {@link com.dotcms.experiments.model.Goals} was success</li>
 *     <li>How many times the {@link com.dotcms.experiments.model.Goals} was success no matter
 *     if it was success several times in the sae session</li>
 * </ul>
 *
 */
public class ExperimentResults {

    private int totalSessions;

    private List<GoalResult> goals;

    private ExperimentResults(int totalSessions, final List<GoalResult> goalResults) {
        this.totalSessions = totalSessions;
        this.goals = goalResults;
    }

    /**
     * Return How many Session the {@link com.dotcms.experiments.model.Experiment}'s Page was view,
     * put in another way how many Session go into the Experiment.
     *
     * @return
     */
    public int getTotalSessions() {
        return totalSessions;
    }

    /**
     * Return the result split by {@link com.dotcms.variant.model.Variant}.
     *
     *
     * @return
     */
    public List<GoalResult> getGoalResults() {
        return goals;
    }

    public static class Builder {

        private int totalSessions;
        private List<GoalResult> goals = new ArrayList<>();
        private Collection<ExperimentVariant> variants;

        public Builder setSessionTotal(final int totalSessions) {
            this.totalSessions = totalSessions;
            return this;
        }

        public Builder addGoal(final Metric goal) {
            this.goals.add(new GoalResult(goal, variants));
            return this;
        }

        public ExperimentResults build() {
            return new ExperimentResults(totalSessions, goals);
        }

        public Builder addVariants(final Collection<ExperimentVariant> variants) {
            this.variants = variants;
            return this;
        }

        public Builder count(final Metric goal, final String lookBackWindow, final Event event) {
            final GoalResult goalresult = this.goals.stream()
                    .filter(goalResult -> goalResult.goal.name().equals(goal.name()))
                    .limit(1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Goal does not exists"));

            goalresult.count(lookBackWindow, event);
            return this;
        }
    }

    public static class GoalResult {

        private Metric goal;
        final Map<String, VariantResult> variants;
        public GoalResult(final Metric goal, final Collection<ExperimentVariant> variants) {
            this.goal = goal;
            this.variants = variants.stream()
                    .collect(Collectors.toMap(ExperimentVariant::id,
                            variant -> new VariantResult(variant.id())));
        }

        public Metric getGoal() {
            return goal;
        }

        public Map<String, VariantResult> getVariants() {
            return variants;
        }

        public void count(final String lookBackWindow, final Event event) {
            final String variantName = event.get("variant")
                    .map(value -> value.toString())
                    .orElseThrow(() -> new IllegalArgumentException("Attribute variant does not exists"));

            final VariantResult variantResult = variants.get(variantName);

            if (variantResult == null) {
                throw new IllegalArgumentException(String.format("Variant %s does not exists in the Experiment", variantName));
            }

            variantResult.count(lookBackWindow, event);
        }
    }

    public static class VariantResult {

        private String variantName;
        private Map<String, List<Event>> events = new HashMap<>();

        public VariantResult(final String variantName) {
            this.variantName = variantName;
        }

        public String getVariantName() {
            return variantName;
        }

        public void count(final String lookBackWindow, final Event event) {
            final List<Event> sessionEvents = UtilMethods.isSet(events.get(lookBackWindow)) ?
                    events.get(lookBackWindow) : createEventsList(lookBackWindow);

            sessionEvents.add(event);
        }

        private  ArrayList<Event> createEventsList(final String lookBackWindow) {
            final ArrayList<Event> list = new ArrayList<>();
            events.put(lookBackWindow, list);
            return list;
        }

        /**
         * Return how many sessions the {@link com.dotcms.experiments.model.Goals} was success.
         *
         * @return
         */
        public int totalUniqueBySession() {
            return events.size();
        }

        /**
         * Return How many times the {@link com.dotcms.experiments.model.Goals} was success no matter
         * if it was success several times in the same session when the user was using this Variant
         *
         * @return
         */
        public int totalMultiBySession() {
            return events.values().stream()
                    .map(sessionEvents -> sessionEvents.size())
                    .mapToInt(Integer::intValue)
                    .sum();
        }
    }
}
