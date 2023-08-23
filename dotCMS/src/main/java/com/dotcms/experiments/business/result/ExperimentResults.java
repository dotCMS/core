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
            totalSessionsBuilder.count(variantName, browserSession);
        }

        public void trafficProportion(final TrafficProportion trafficProportion) {
            this.trafficProportion = trafficProportion;
        }

        private static class TotalSessionBuilder {

            private final Map<String, SessionDetail> totalSessions = new HashMap<>();
            private final List<String> variantsName;

            public TotalSessionBuilder(Collection<ExperimentVariant> variantsName) {
                this.variantsName = variantsName.stream()
                        .map(experimentVariant -> experimentVariant.id())
                        .collect(Collectors.toList());

                for (final String variantName : this.variantsName) {
                    totalSessions.put(variantName, new SessionDetail());
                }
            }

            public void count(final String variantName, BrowserSession browserSession) {
                DotPreconditions.isTrue(variantsName.contains(variantName), "Variant does not exists");

                final SessionDetail sessionDetail = totalSessions.getOrDefault(variantName,
                        new SessionDetail());

                sessionDetail.count(browserSession.getDate().orElseThrow());
                totalSessions.put(variantName, sessionDetail);
            }

            private long getTotal() {
                return totalSessions.values().stream().mapToLong(SessionDetail::getTotal).sum();
            }

            public TotalSession build(){
                final TotalSessionByVariant totalSessionByVariant = new TotalSessionByVariant();

                for (String variantName : variantsName) {
                    final SessionDetail sessionDetail = totalSessions.get(variantName);

                    if (sessionDetail == null) {
                        continue;
                    }

                    totalSessionByVariant.put(variantName, sessionDetail.getDetails());
                }

                return new TotalSession(getTotal(), totalSessionByVariant);
            }

            /**
             * Util class to count the number of session per day and Variant.
             */
            private class SessionDetail {

                final Map<Instant, Long> details = new TreeMap<>(new Comparator<Instant>() {
                    @Override
                    public int compare(Instant instant1, Instant instant2) {
                        // Extract date, month, and year components from the Instants
                        int year1 = instant1.atZone(ZoneOffset.UTC).getYear();
                        int month1 = instant1.atZone(ZoneOffset.UTC).getMonthValue();
                        int day1 = instant1.atZone(ZoneOffset.UTC).getDayOfMonth();

                        int year2 = instant2.atZone(ZoneOffset.UTC).getYear();
                        int month2 = instant2.atZone(ZoneOffset.UTC).getMonthValue();
                        int day2 = instant2.atZone(ZoneOffset.UTC).getDayOfMonth();

                        // Compare year, then month, then day
                        if (year1 != year2) {
                            return Integer.compare(year1, year2);
                        }
                        if (month1 != month2) {
                            return Integer.compare(month1, month2);
                        }
                        return Integer.compare(day1, day2);
                    }
                });
                public void count(final Instant date) {
                    final Long count = details.getOrDefault(date, 0L);
                    details.put(date, count + 1);
                }

                public long getTotal() {
                    return details.values().stream().mapToLong(Long::longValue).sum();
                }

                public Map<Instant, Long> getDetails() {
                    return details;
                }
            }
        }

    }

    /**
     * Represent the number of session per day and Variant.
     */
    public static class TotalSessionByVariant {
        final Map<String, Map<Instant, Long>> sessionsResult = new HashMap<>();

        public void put(final String variantName, final Map<Instant, Long> details) {
            sessionsResult.put(variantName, details);
        }

        public Map<String, Long> getTotalByVariants() {
            final Map<String, Long> totalByVariants = new HashMap<>();

            for (Entry<String, Map<Instant, Long>> entry : sessionsResult.entrySet()) {
                final Long total = entry.getValue().values().stream().mapToLong(Long::longValue).sum();
                totalByVariants.put(entry.getKey(), total);
            }

            return totalByVariants;
        }

        public long getTotalByVariant(final String variantName, final Instant date) {
            final Map<Instant, Long> details = sessionsResult.get(variantName);

            if (details == null) {
                return 0;
            }

            final Long total = details.get(date);
            return UtilMethods.isSet(total) ? total : 0;
        }

        public long getTotalByVariant(final String variantName) {
            final Map<Instant, Long> details = sessionsResult.get(variantName);

            if (details == null) {
                return 0;
            }

            return details.values().stream().mapToLong(Long::longValue).sum();
        }

        public long getTotalByDate(final Instant date) {
            return sessionsResult.values().stream()
                    .flatMap(details -> details.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(date)))
                    .map(entry -> entry.getValue())
                    .mapToLong(Long::longValue)
                    .sum();
        }
    }


    public static class TotalSession {

        private final long total;
        private final TotalSessionByVariant totalSessionByVariant;

        public TotalSession(final long total, final TotalSessionByVariant variants) {
            this.total = total;
            this.totalSessionByVariant = variants;
        }

        public long getTotalByDate(final Instant date) {
            return totalSessionByVariant.getTotalByDate(date);
        }

        public long getTotal() {
            return total;
        }

        public long getTotal(final String variantName) {
            return totalSessionByVariant.getTotalByVariant(variantName);
        }

        public long getTotal(final String variantName, final Instant date) {
            return totalSessionByVariant.getTotalByVariant(variantName, date);
        }

        public Map<String, Long> getVariants() {
            return totalSessionByVariant.getTotalByVariants();
        }

    }

}
