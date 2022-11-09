package com.dotcms.experiments.business.web;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * WebAPI to {@link Experiment}
 */
public interface ExperimentWebAPI {

    SelectedExperiment NONE_EXPERIMENT = new SelectedExperiment("NONE", "NONE", null,
            new SelectedVariant("NONE", null));

    /**
     * Return if the current user should be included into a RUNNING {@link Experiment}:
     *
     * - First it checks it the {@link Experiment#targetingConditions()} is valid for the user or current
     * {@link HttpServletRequest}.
     * - Then it use the {@link Experiment#trafficAllocation()} to know if the user should go into the
     * {@link Experiment}.
     * - Finally it assing a {@link com.dotcms.experiments.model.ExperimentVariant} according to
     * {@link com.dotcms.experiments.model.ExperimentVariant#weight()}
     *
     * If exists more that one {@link Experiment} RUNNING it try to get the user into any of them
     * one by one if finally the user is not going into any experiment then it return a
     * {@link com.dotcms.experiments.business.web.ExperimentWebAPI#NONE_EXPERIMENT}
     *
     * @param request current HTTP Request
     * @param response current HTTP Reponse
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    SelectedExperiment isUserIncluded(final HttpServletRequest request, final HttpServletResponse response)
            throws DotDataException, DotSecurityException;
}
