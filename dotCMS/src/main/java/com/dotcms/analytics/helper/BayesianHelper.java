package com.dotcms.analytics.helper;

import com.dotcms.analytics.bayesian.BayesianAPI;
import com.dotcms.analytics.bayesian.model.ABTestingType;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.VariantInput;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.GoalResults;
import com.dotcms.experiments.business.result.VariantResults;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;

/**
 * Bayesian helper class with convenience methods to transition from Experiments realm to Bayesian.
 *
 * @author vico
 */
public class BayesianHelper {

    private static final Lazy<BayesianHelper> bayesianHelper = Lazy.of(BayesianHelper::new);

    public static BayesianHelper get(){
        return bayesianHelper.get();
    }

    private BayesianHelper() {}

    /**
     * Converts {@link ExperimentResults} to {@link BayesianInput} object
     *
     * @param experimentResults experiment results
     * @param goalName goal name to get results from
     * @return {@link BayesianInput} bayesian input object
     */
    public BayesianInput toBayesianInput(final ExperimentResults experimentResults, final String goalName) {
        return extractGoalResults(experimentResults, goalName)
            .map(results -> {
                final VariantResults controlResults = results.getVariants().get(DEFAULT_VARIANT.name());
                if (Objects.isNull(controlResults)) {
                    Logger.error(this, "Control results are missing");
                    return null;
                }

                final List<VariantResults> variantsResults = results
                    .getVariants()
                    .keySet()
                    .stream()
                    .filter(BayesianAPI.OTHER_THAN_DEFAULT_VARIANT_FILTER)
                    .map(name -> results.getVariants().get(name))
                    .collect(Collectors.toList());
                if (variantsResults.isEmpty()) {
                    Logger.error(this, "Variants results are missing");
                    return null;
                }

                return BayesianInput
                    .builder()
                    .type(variantsResults.size() == 1 ? ABTestingType.AB: ABTestingType.ABC)
                    .control(toVariantBayesianInput(experimentResults, controlResults, true))
                    .variantPairs(
                        variantsResults
                            .stream()
                            .map(vr -> toVariantBayesianInput(experimentResults, vr, false))
                            .collect(Collectors.toList()))
                    .priors(resolvePriors(goalName, experimentResults))
                    .build();
            })
            .orElse(null);
    }

    /**
     * Resolves priors to use for Bayesian analysis.
     * Currently, they are null since we don't support it yet.
     *
     * @param goalName goal name to get results from
     * @param experimentResults experiment results
     * @return resolved priors
     */
    private List<BayesianPriors> resolvePriors(String goalName, final ExperimentResults experimentResults) {
        return List.of(experimentResults
                .getGoals()
                .get(goalName)
                .getVariants()
                .keySet()
                .stream()
                .map(name -> BayesianAPI.DEFAULT_PRIORS)
                .toArray(BayesianPriors[]::new));
    }

    /**
     * Resolves GoalResults from {@link ExperimentResults} object.
     *
     * @param experimentResults experiment results
     * @param goalName goal name to get results from
     * @return
     */
    private Optional<GoalResults> extractGoalResults(final ExperimentResults experimentResults, final String goalName) {
        return Optional.ofNullable(experimentResults.getGoals().get(goalName));
    }

    /**
     * Extracts successes from {@link VariantResults} object
     *
     * @param variantResults variant results
     * @return successes
     */
    private long extractSuccesses(final VariantResults variantResults) {
        return variantResults.getUniqueBySession().getCount();
    }

    /**
     * Extracts failures from {@link ExperimentResults} object
     *
     * @param experimentResult experiment results
     * @param variantResults variant results
     * @param successes successes
     * @return failures
     */
    private long extractFailures(final ExperimentResults experimentResult,
                                 final VariantResults variantResults,
                                 final long successes) {
        return Optional
            .ofNullable(experimentResult.getSessions().getVariants().get(variantResults.getVariantName()))
            .filter(total -> total != 0)
            .map(total -> total - successes)
            .orElse(0L);
    }

    /**
     * Converts {@link ExperimentResults} to {@link BayesianInput} object
     *
     * @param experimentResults experiment results
     * @param variantResults variant results
     * @param isControl is control variant flag
     * @return {@link BayesianInput} bayesian input object
     */
    private VariantInput toVariantBayesianInput(final ExperimentResults experimentResults,
                                                final VariantResults variantResults,
                                                final boolean isControl) {
        final long successes = extractSuccesses(variantResults);
        return VariantInput.builder()
            .variant(variantResults.getVariantName())
            .successes(successes)
            .failures(extractFailures(experimentResults, variantResults, successes))
            .isControl(isControl)
            .build();
    }

}
