package com.dotcms.analytics.bayesian;


import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.variant.VariantAPI;

import java.util.List;
import java.util.function.Predicate;

/**
 * Bayesian calculation API.
 *
 * @author vico
 */
public interface BayesianAPI {

    String TIE = "_TIE_";
    String NONE = "_NONE_";
    BayesianPriors NULL_PRIORS = BayesianPriors.builder().alpha(null).beta(null).build();
    Predicate<String> DEFAULT_VARIANT_FILTER = variant -> variant.equals(VariantAPI.DEFAULT_VARIANT.name());
    Predicate<String> OTHER_THAN_DEFAULT_VARIANT_FILTER = variant -> DEFAULT_VARIANT_FILTER.negate().test(variant);
    BayesianResult NOOP_RESULT = BayesianResult.builder().probabilities(List.of()).suggested(NONE).build();

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

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) based on this pseudo (Julia) code:
     *
     * <pre>
     *    function probability_C_beats_A_and_B(α_A, β_A, α_B, β_B, α_C, β_C)
     *        total = 0.0
     *        for i = 0:(α_A-1)
     *            for j = 0:(α_B-1)
     *                total += exp(logbeta(α_C+i+j, β_A+β_B+β_C) - log(β_A+i) - log(β_B+j)
     *                    - logbeta(1+i, β_A) - logbeta(1+j, β_B) - logbeta(α_C, β_C))
     *            end
     *        end
     *        return (1 - probability_B_beats_A(α_C, β_C, α_A, β_A)
     *            - probability_B_beats_A(α_C, β_C, α_B, β_B) + total)
     *    end
     * </pre>
     *
     * Instead of using the provided logBeta function from Apache Commons Math we will use our own implementation:
     * {@link com.dotcms.analytics.bayesian.BetaDistribution} which provides en density function {@see DotBetaDistribution.pdf()}.
     *
     * @param input {@link com.dotcms.analytics.bayesian.model.BayesianInput} instance
     */
    BayesianResult calcProbABC(BayesianInput input);

}
