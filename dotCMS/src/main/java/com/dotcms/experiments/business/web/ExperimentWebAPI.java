package com.dotcms.experiments.business.web;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * WebAPI to {@link Experiment}
 */
public interface ExperimentWebAPI {

    SelectedExperiment NONE_EXPERIMENT = new SelectedExperiment("NONE", "NONE", null,
            new SelectedVariant("NONE", null));

    /**
     * Return if the current user should be included into a set of RUNNING {@link Experiment}:
     *
     * - First it checks it the {@link Experiment#targetingConditions()} is valid for the user or current
     * {@link HttpServletRequest}.
     * - Then it uses the {@link Experiment#trafficAllocation()} to know if the user should go into the
     * {@link Experiment}.
     * - Finally it set a {@link com.dotcms.experiments.model.ExperimentVariant} according to
     * {@link com.dotcms.experiments.model.ExperimentVariant#weight()}
     *
     * If exists more than one {@link Experiment} RUNNING it try to get the user into any of them
     * one by one just excluding the ones in the idsToExclude parameter if finally the user is not
     * going into any experiment then it return a
     * {@link com.dotcms.experiments.business.web.ExperimentWebAPI#NONE_EXPERIMENT}
     *
     * Also, it is possible that the User get into several {@link Experiment}.
     *
     * @param request current HTTP Request
     * @param response current HTTP Reponse
     * @param idsToExclude {@link Experiment}' is to excluded, null or empty list mean that none is excluded
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    SelectedExperiments isUserIncluded(final HttpServletRequest request,
            final HttpServletResponse response, final List<String> idsToExclude) throws DotDataException, DotSecurityException ;
}
