package com.dotcms.experiments.business.web;


import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a set of {@link SelectedExperiment} selected to a User, this mean this user
 * is going to be into all these experiments {@link com.dotcms.experiments.model.Experiment}.
 *
 * @see ExperimentWebAPI#isUserIncluded(HttpServletRequest, HttpServletResponse)
 * @see com.dotcms.experiments.model.Experiment
 */
public class SelectedExperiments {
    private List<SelectedExperiment> experiments;
    private List<String> includeExperimentIds;

    public SelectedExperiments(
            final List<SelectedExperiment> experiments,
            final List<String> includeExperimentIds) {
        this.experiments = experiments;
        this.includeExperimentIds = includeExperimentIds;
    }

    public List<SelectedExperiment> getExperiments() {
        return experiments;
    }

    public List<String> getIncludeExperimentIds() {
        return includeExperimentIds;
    }
}
