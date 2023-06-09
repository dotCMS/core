package com.dotcms.experiments.business.result;

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
    ;
    private String variantName;
    private String variantDescription;
    private long multiBySession;
    private UniqueBySessionResume uniqueBySession;
    private Map<String, ResultResumeItem> details;
    private long totalPageViews;

    public VariantResults(final String variantName, final String description, final long multiBySession,
            final UniqueBySessionResume uniqueBySession, final Map<String, ResultResumeItem> details,
            final long totalPageViews, float weight) {
        this.variantName = variantName;
        this.multiBySession = multiBySession;
        this.uniqueBySession = uniqueBySession;
        this.details = details;
        this.variantDescription = description;
        this.totalPageViews = totalPageViews;
        this.weight = weight;
    }

    public String getVariantDescription() {
        return variantDescription;
    }

    public String getVariantName() {
        return variantName;
    }

    public long getMultiBySession() {
        return multiBySession;
    }

    public UniqueBySessionResume getUniqueBySession() {
        return uniqueBySession;
    }

    public Map<String, ResultResumeItem> getDetails() {
        return details;
    }

    public long getTotalPageViews() {
        return totalPageViews;
    }

    public float weight() {
        return weight;
    }

    public float getWeight() {
        return weight;
    }

    public static class UniqueBySessionResume {

        private final long count;
        private final float totalPercentage;
        private final float variantPercentage;

        public UniqueBySessionResume(final int count, final long totalVariantSession, long totalSessions) {
            this.count = count;
            this.totalPercentage = totalSessions > 0 ? (float) (count * 100) / totalSessions : 0;
            this.variantPercentage = totalVariantSession > 0 ? (float) (count * 100) / totalVariantSession : 0;
        }

        public long getCount() {
            return count;
        }

        public float getTotalPercentage() {
            return totalPercentage;
        }

        public float getVariantPercentage() {
            return variantPercentage;
        }
    }

    public static class ResultResumeItem {
        final long multiBySession;
        final long uniqueBySession;

        public ResultResumeItem(final long multiBySession, final long uniqueBySession) {
            this.multiBySession = multiBySession;
            this.uniqueBySession = uniqueBySession;
        }

        public long getMultiBySession() {
            return multiBySession;
        }

        public long getUniqueBySession() {
            return uniqueBySession;
        }
    }
}
