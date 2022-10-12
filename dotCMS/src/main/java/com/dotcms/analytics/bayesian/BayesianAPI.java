package com.dotcms.analytics.bayesian;


import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianResult;

/**
 * Bayesian calculation API.
 *
 * @author vico
 */
public interface BayesianAPI {

    BayesianResult calcABTesting(BayesianInput input);

}
