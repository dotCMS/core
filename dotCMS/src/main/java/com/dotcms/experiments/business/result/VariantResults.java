package com.dotcms.experiments.business.result;

import com.dotcms.experiments.model.ExperimentVariant;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent a partial or total Result for a specific {@link com.dotcms.experiments.model.ExperimentVariant}
 * into a {@link com.dotcms.experiments.model.Experiment}.
 *
 * The VariantResult include the follow data for a specific variant:
 *
 * <ul>
 *     <li>How many Sessions the {@link com.dotcms.experiments.model.Experiment}'s Goal was success</li>
 *     <li>How many times the {@link com.dotcms.experiments.model.Experiment}'s Goal was success no matter
 *     if it was success several times in the sae session</li>
 * </ul>
 *
 */
public class VariantResults {

    private final float weight;

    private String variantName;
    private String variantDescription;
    private UniqueBySessionResume uniqueBySession;
    private Map<String, ResultResumeItem> details;

    public VariantResults(final Builder builder) {
        this.variantName = builder.experimentVariant.id();
        this.uniqueBySession = builder.uniqueBySession != null ? builder.uniqueBySession : new UniqueBySessionResume(0, 0);
        this.details = builder.details;
        this.variantDescription = builder.experimentVariant.description();
        this.weight = builder.experimentVariant.weight();
    }

    public String getVariantDescription() {
        return variantDescription;
    }

    public String getVariantName() {
        return variantName;
    }

    public UniqueBySessionResume getUniqueBySession() {
        return uniqueBySession;
    }

    public Map<String, ResultResumeItem> getDetails() {
        return details;
    }

    public float weight() {
        return weight;
    }

    public float getWeight() {
        return weight;
    }

    public static class UniqueBySessionResume {

        private final long count;
        private final float conversionRate;

        public UniqueBySessionResume(final long count, final float conversionRate) {
            this.count = count;
            this.conversionRate = conversionRate;
        }

        public long getCount() {
            return count;
        }

        public float getConversionRate() {
            return conversionRate;
        }
    }

    public static class ResultResumeItem {
        final long uniqueBySession;
        final long totalSessions;
        final float convertionRate;


        public ResultResumeItem(final long uniqueBySession, final long totalSessions, final float convertionRate) {
            this.convertionRate = convertionRate;
            this.uniqueBySession = uniqueBySession;
            this.totalSessions = totalSessions;
        }

        public long getUniqueBySession() {
            return uniqueBySession;
        }

        public float getConversionRate() {
            return convertionRate;
        }

        public long getTotalSessions() {
            return totalSessions;
        }
    }

    static class Builder {

        ExperimentVariant experimentVariant;
        UniqueBySessionResume uniqueBySession;
        Map<String, ResultResumeItem> details = new HashMap<>();

        Builder(final ExperimentVariant experimentVariant) {
            this.experimentVariant = experimentVariant;
        }


        public void uniqueBySession(UniqueBySessionResume uniqueBySession) {
            this.uniqueBySession = uniqueBySession;
        }


        public void add(final String day, final ResultResumeItem resultResumeItem) {
            details.put(day, resultResumeItem);
        }

        public VariantResults build() {
            return new VariantResults(this);
        }

        public void addIfNotExists(final String day, final ResultResumeItem resultResumeItemToAdd) {
            final ResultResumeItem resultResumeItem = details.get(day);

            if (resultResumeItem == null) {
                details.put(day, resultResumeItemToAdd);
            }
        }
    }
}
