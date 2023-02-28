package com.dotcms.analytics.bayesian;


import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;

/**
 * Bayesian calculation API.
 *
 * @author vico
 */
public interface BayesianAPI {

    BayesianPriors NULL_PRIORS = BayesianPriors.builder().alpha(null).beta(null).build();

    /**
     * Calculates probability that B (Test) beats A (Control) based on this pseudo (Julia) code:
     *
     * <pre>
     *     𝛼𝐴 is one plus the number of successes for A
     *     𝛽𝐴 is one plus the number of failures for A
     *     𝛼𝐵 is one plus the number of successes for B
     *     𝛽𝐵 is one plus the number of failures for B
     *
     *     function probability_B_beats_A(α_A, β_A, α_B, β_B)
     *         total = 0.0
     *         for i = 0:(α_B-1)
     *             total += exp(logbeta(α_A+i, β_B+β_A)
     *                 - log(β_B+i) - logbeta(1+i, β_B) - logbeta(α_A, β_A))
     *         end
     *         return total
     *     end
     * </pre>
     *
     * Instead of using the provided logBeta function from Apache Commons Math we will use our own implementation:
     * {@link BetaDistribution} which provides en density function {@see DotBetaDistribution.pdf()}.
     *
     * @param input {@link BayesianInput} instance
     */
    BayesianResult calcProbBOverA(BayesianInput input);

}
