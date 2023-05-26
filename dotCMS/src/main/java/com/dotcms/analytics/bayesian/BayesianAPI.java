package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.bayesian.model.VariantResult;
import com.dotcms.variant.VariantAPI;
import org.apache.commons.numbers.gamma.LogBeta;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Bayesian calculation API that defines the methods to calculate the Bayesian results from a provided set of inputs.
 * Basically iw defines one method that receive a BayesianInput object and returns a BayesianResult object and a set of
 * constants required to be consumed by implementing classes.
 *
 * @author vico
 */
public interface BayesianAPI {

    double DEFAULT_ALPHA = 1.0;
    double DEFAULT_BETA = 1.0;
    double LOWER_BOUND_PROBABILITY = 0.025;
    double UPPER_BOUND_PROBABILITY = 0.975;
    double BETA_DIST_DEFAULT = (double) 3 / 8;
    double HALF = 0.5;
    double TIE_COMPARE_DELTA = 0.01;
    double[] QUANTILES = { 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 0.75, 0.9, 0.95, 0.975, 0.99 };
    String TIE = "TIE";
    String NONE = "NONE";
    BayesianPriors DEFAULT_PRIORS = BayesianPriors.builder()
        .alpha(DEFAULT_ALPHA)
        .beta(DEFAULT_BETA)
        .build();
    BayesianResult NOOP_RESULT = BayesianResult.builder()
        .value(0.0)
        .results(List.of())
        .suggestedWinner(NONE)
        .build();
    BiFunction<Double, Double, Double> LOG_BETA_FN = LogBeta::value;
    Comparator<VariantResult> VARIANT_RESULT_COMPARATOR = Comparator.comparingDouble(VariantResult::probability);
    Predicate<String> DEFAULT_VARIANT_FILTER = variant -> variant.equals(VariantAPI.DEFAULT_VARIANT.name());
    Predicate<String> OTHER_THAN_DEFAULT_VARIANT_FILTER = variant -> DEFAULT_VARIANT_FILTER.negate().test(variant);

    /**
     * Calculates probability that each variant beats the control including a credibility interval and calculated risk.
     * Instead of using the provided logBeta function from Apache Commons Math we will use our own implementation:
     *
     * @param input {@link BayesianInput} instance
     */
    BayesianResult doBayesian(BayesianInput input);

}
