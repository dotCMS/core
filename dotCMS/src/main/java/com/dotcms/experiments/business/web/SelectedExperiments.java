package com.dotcms.experiments.business.web;


import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a set of {@link SelectedExperiment} selected to a User, this mean this user
 * is going to be into all these experiments {@link com.dotcms.experiments.model.Experiment}.
 *
 * @see ExperimentWebAPI#isUserIncluded(HttpServletRequest, HttpServletResponse, List)
 * @see com.dotcms.experiments.model.Experiment
 */
public class SelectedExperiments {
    private List<SelectedExperiment> experiments;
    private List<String> includedExperimentIds;
    private List<String> excludedExperimentIds;

    private List<String> excludedExperimentIdsEnded;

    private SelectedExperiments(final Builder builder) {
        this.experiments = builder.experiments;
        this.includedExperimentIds = builder.included;
        this.excludedExperimentIds = builder.excluded;
        this.excludedExperimentIdsEnded = builder.excludedExperimentIdsEnded;
    }

    public List<SelectedExperiment> getExperiments() {
        return experiments;
    }

    public List<String> getIncludedExperimentIds() {
        return includedExperimentIds;
    }

    public List<String> getExcludedExperimentIds() {
        return excludedExperimentIds;
    }

    public List<String> getExcludedExperimentIdsEnded() {
        return excludedExperimentIdsEnded;
    }

    public static class Builder {
        private List<SelectedExperiment> experiments;
        private List<String> included;
        private List<String> excluded;

        private List<String> excludedExperimentIdsEnded;

        public Builder experiments(List<SelectedExperiment> experiments) {
            this.experiments = experiments;
            return this;
        }

        public Builder included(List<String> included) {
            this.included = included;
            return this;
        }

        public Builder excluded(List<String> excluded) {
            this.excluded = excluded;
            return this;
        }

        public Builder excludedExperimentIdsEnded(List<String> excludedExperimentIdsEnded) {
            this.excludedExperimentIdsEnded = excludedExperimentIdsEnded;
            return this;
        }

        public SelectedExperiments build() {
            return new SelectedExperiments(this);
        }
    }
}
