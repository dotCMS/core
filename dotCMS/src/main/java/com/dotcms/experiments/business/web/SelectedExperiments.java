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

    public SelectedExperiments(
            final List<SelectedExperiment> experiments,
            final List<String> includeExperimentIds,
            final List<String> excludedExperimentIds
    ) {
        this.experiments = experiments;
        this.includedExperimentIds = includeExperimentIds;
        this.excludedExperimentIds = excludedExperimentIds;
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
}
