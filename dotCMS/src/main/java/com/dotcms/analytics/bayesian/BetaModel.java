package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.model.BayesianInput;


/**
 * Beta Distribution wrapping class.
 *
 * @author vico
 */
public class BetaModel {

    private Double alpha;
    private Double beta;

    public BetaModel(final BayesianInput input) {
        setValues(input.priors().alpha(), input.priors().beta());
    }

    /**
     * Adds provided successes and failures values to current alpha and beta attributes.
     *
     * @param successes number os successes
     * @param failures number of failures
     */
    public void update(final int successes, final int failures) {
        setValues(alpha + successes, beta + failures);
    }

    /**
     * Creates a new instance of {@link BetaDistribution} with already provided alpha and beta values.
     *
     * @return a beta distribution object.
     */
    public BetaDistribution distribution() {
        return BetaDistribution.create(alpha, beta);
    }

    /**
     * Set alpha and beta values.
     *
     * @param alpha alpha value
     * @param beta beta value
     */
    private void setValues(final Double alpha, final Double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

}
