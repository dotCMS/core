package com.dotcms.analytics.bayesian;


import com.dotcms.analytics.bayesian.model.ABTestingType;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.bayesian.model.DifferenceData;
import com.dotcms.analytics.bayesian.model.QuantilePair;
import com.dotcms.analytics.bayesian.model.SampleData;
import com.dotcms.analytics.bayesian.model.SampleGroup;
import com.dotcms.analytics.bayesian.model.VariantInputPair;
import com.dotcms.analytics.bayesian.model.VariantProbability;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.numbers.gamma.LogBeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Bayesian calculation API.
 * So far it implements on method that calculates the probability for an A/B test that B (test) beats A (control).
 *
 * @author vico
 */
public class BayesianAPIImpl implements BayesianAPI {

    private static final double DEFAULT_VALUE = (double) 3 / 8;
    private static final double[] QUANTILES = { 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 0.75, 0.9, 0.95, 0.975, 0.99 };
    private static final int SAMPLE_SIZE = Config.getIntProperty("BETA_DISTRIBUTION_SAMPLE_SIZE", 1000);
    private static final BiFunction<Double, Double, Double> LOG_BETA_FN = LogBeta::value;
    private static final double HALF = 0.5;
    private static final double TIE_COMPARE_DELTA = 0.01;
    private static final Comparator<VariantProbability> VARIANT_PROBABILITY_COMPARATOR =
        Comparator.comparingDouble(VariantProbability::value);

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianResult calcProbBOverA(final BayesianInput input) {
        // validate input
        final Optional<BayesianResult> noopResult = detectNoop(input);
        if (noopResult.isPresent()) {
            return noopResult.get();
        }

        final boolean priorPresent = isPriorPresent(input);
        final BetaDistribution controlData = priorPresent ? new BetaModel(input).distribution() : null;
        final BetaDistribution testData = priorPresent ? new BetaModel(input).distribution() : null;
        final DifferenceData differenceData = generateDifferenceData(controlData, testData);
        final VariantInputPair control = input.control();
        final VariantInputPair test = input.variantPairs().get(0);
        // call calculation
        final double value = calcABTesting(control, test);

        BayesianResult.Builder builder = BayesianResult.builder()
            .value(value)
            .probabilities(List.of(
                toProbability(input.control(), 1 - value),
                toProbability(input.variantPairs().get(0), value)))
            .suggestedWinner(suggestABWinner(value, control.variant(), input.variantPairs().get(0).variant()));
        if (priorPresent) {
            builder = builder
                .distributionPdfs(calcDistributionsPdfs(controlData, testData))
                .differenceData(differenceData)
                .quantiles(calcQuantiles(
                    differenceData.differences(),
                    input.priors().alpha(),
                    input.priors().beta()));
        }

        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianResult calcProbABC(final BayesianInput input) {
        // validate input
        final Optional<BayesianResult> noopResult = detectNoop(input);
        if (noopResult.isPresent()) {
            return noopResult.get();
        }

        final List<VariantProbability> results = calcABCTesting(
            input.control(),
            input.variantPairs().get(0),
            input.variantPairs().get(1));

        return BayesianResult.builder()
            .value(results.get(0).value())
            .probabilities(results)
            .suggestedWinner(suggestABCWinner(results))
            .build();
    }

    /**
     * Resolves which variant could be considered as the winner of the test.
     *
     * @param value calculated probability that B (Test) beats A (Control)
     * @param control control variant
     * @param test test variant
     * @return winner variant name
     */
    @NotNull
    private String suggestABWinner(final double value, final String control, final String test) {
        if (Double.compare(HALF, value) == 0 || Math.abs(HALF - value) <= TIE_COMPARE_DELTA) {
            return TIE;
        }

        return value < 0.5 ? control : test;
    }

    /**
     * Resolves which variant could be considered as the winner of the test.
     *
     * @param probabilities list of probabilities
     * @return winner variant name
     */
    @NotNull
    private String suggestABCWinner(final List<VariantProbability> probabilities) {
        final VariantProbability controlProbability = probabilities.get(0);
        if (probabilities
            .stream()
            .allMatch(variantProbability -> variantProbability.value() == controlProbability.value())) {
            return TIE;
        }

        return probabilities
            .stream()
            .max(VARIANT_PROBABILITY_COMPARATOR)
            .map(VariantProbability::variant)
            .orElse(NONE);
    }

    /**
     * Calculates probability that B (Test) beats A (Control) just as main {@link #calcProbBOverA(BayesianInput)}
     *
     * @param alphaA control successes
     * @param betaA control failures
     * @param alphaB test successes
     * @param betaB test failures
     * @return result representing probability that B beats A
     */
    private double calcABTesting(final long alphaA, final long betaA, final long alphaB, final long betaB) {
        double result = 0.0;
        for(int i = 0; i < alphaB; i++) {
            result += Math.exp(
                logBeta(alphaA + i, betaB + betaA)
                    - Math.log(betaB + i)
                    - logBeta(1 + i, betaB)
                    - logBeta(alphaA, betaA));
        }
        return result;
    }

    /**
     * Calculates probability that B (Test) beats A (Control) just as main {@link #calcProbBOverA(BayesianInput)}
     *
     * @param control successes and failures
     * @param test successes and failures
     * @return result representing probability that B beats A
     */
    private double calcABTesting(final VariantInputPair control, final VariantInputPair test) {
        return calcABTesting(
            control.successes() + 1,
            control.failures() + 1,
            test.successes() + 1,
            test.failures() + 1);
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) just as main
     * {@link #calcProbABC(BayesianInput)}
     *
     * @param alphaA control successes
     * @param betaA control failures
     * @param alphaB test B successes
     * @param betaB test B failures
     * @param alphaC test C successes
     * @param betaC test C failures
     * @return a list {@link VariantProbability} representing probability that C beats A and B
     */
    private Double calcABCTesting(final long alphaA, final long betaA,
                                  final long alphaB, final long betaB,
                                  final long alphaC, final long betaC) {
        double total = 0.0;

        for(int i = 0; i < alphaA; i++) {
            for(int j = 0; j < alphaB; j++) {
                total += Math.exp(
                    logBeta(alphaC + i + j, betaA + betaB + betaC)
                        - Math.log(betaA + i)
                        - Math.log(betaB + j)
                        - logBeta(1 + i, betaA)
                        - logBeta(1 + j, betaB)
                        - logBeta(alphaC, betaC));
            }
        }

        final double probAOverC = calcABTesting(alphaC, betaC, alphaA, betaA);
        final double probBOverC = calcABTesting(alphaC, betaC, alphaB, betaB);

        return 1 - probAOverC - probBOverC + total;
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) just as main
     * {@link #calcProbABC(BayesianInput)}
     *
     * @param control successes and failures
     * @param testB successes and failures
     * @param testC successes and failures
     * @return a list {@link VariantProbability} representing probability that C beats A and B
     */
    private VariantProbability calcSingleABCTesting(final VariantInputPair control,
                                                    final VariantInputPair testB,
                                                    final VariantInputPair testC) {
        return toProbability(
            testC,
            calcABCTesting(
                control.successes() + 1,
                control.failures() + 1,
                testB.successes() + 1,
                testB.failures() + 1,
                testC.successes() + 1,
                testC.failures() + 1));
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) just as main
     * {@link #calcProbABC(BayesianInput)}
     *
     * @param control successes and failures
     * @param testB successes and failures
     * @param testC successes and failures
     * @return a list {@link VariantProbability} representing probability that C beats A and B
     */
    private List<VariantProbability> calcABCTesting(final VariantInputPair control,
                                                    final VariantInputPair testB,
                                                    final VariantInputPair testC) {
        return Stream
            .of(
                List.of(testC, testB, control),
                List.of(control, testC, testB),
                List.of(testB, control, testC))
            .map(inputs -> calcSingleABCTesting(inputs.get(0), inputs.get(1), inputs.get(2)))
            .collect(Collectors.toList());
    }

    /**
     * Converts variant pair information and the calculated probability into {@link VariantProbability} instance.
     *
     * @param pair variant pair
     * @param value calculated probability
     * @return {@link VariantProbability} instance
     */
    private VariantProbability toProbability(final VariantInputPair pair, final double value) {
        return VariantProbability.builder()
            .variant(pair.variant())
            .value(value)
            .build();
    }

    /**
     * Given a {@link BetaDistribution} instance calculates density (pdf) elements.
     *
     * @param distribution provided beta distribution
     * @return list of {@link SampleData} instances
     */
    private List<SampleData> calcPdfElements(final BetaDistribution distribution) {
        return Optional
            .ofNullable(distribution)
            .map(dist -> IntStream
                .range(0, SAMPLE_SIZE)
                .mapToObj(operand -> {
                    final double x = (double) operand / SAMPLE_SIZE;
                    final double temp = dist.pdf(x);
                    final double y = temp == Double.POSITIVE_INFINITY ? 0 : temp;
                    return SampleData.builder()
                        .x(x)
                        .y(y)
                        .build();
                })
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    /**
     * Given a couple {@link BetaDistribution} objects calculates dendity(PDF) for both control (A) and test (B).
     *
     * @param controlDistribution provided control beta distribution
     * @param testDistribution provided test beta distribution
     * @return {@link SampleGroup} containing each sample list against an identifier
     */
    private SampleGroup calcDistributionsPdfs(final BetaDistribution controlDistribution,
                                              final BetaDistribution testDistribution) {
        return SampleGroup.builder()
            .samples(ImmutableMap.of(
                "A", calcPdfElements(controlDistribution),
                "B", calcPdfElements(testDistribution)))
            .build();
    }

    /**
     * Given a couple {@link BetaDistribution} objects generates difference between test and control sample data.
     *
     * @param controlDistribution provided control beta distribution
     * @param testDistribution provided test beta distribution
     * @return {@link  DifferenceData} instance with control and test data and its difference
     */
    private DifferenceData generateDifferenceData(final BetaDistribution controlDistribution,
                                                  final BetaDistribution testDistribution) {
        final double[] controlData = Optional
            .ofNullable(controlDistribution)
            .map(control -> control.rvs(SAMPLE_SIZE))
            .orElse(new double[0]);
        final double[] testData = Optional
            .ofNullable(testDistribution)
            .map(test -> test.rvs(SAMPLE_SIZE))
            .orElse(new double[0]);
        return DifferenceData.builder()
            .controlData(controlData)
            .testData(testData)
            .differences(
                IntStream.range(0, controlData.length)
                    .mapToDouble(i -> testData[i] - controlData[i])
                    .toArray())
            .build();
    }

    /**
     * Gets the maximum number between a given number and the minimum between two other numbers.
     *
     * @param arg number
     * @param min number
     * @param max number
     * @return maximum result
     */
    private double clip(final double arg, final double min, final double max) {
        return Math.max(min, Math.min(arg, max));
    }

    /**
     * Calculates quantiles from a difference data array, an alpha and beta values.
     *
     * @param differences difference data array
     * @param alpha alpha value
     * @param beta beta value
     * @return map of calculated quantiles double values
     */
    private Map<Double, QuantilePair> calcQuantiles(final double[] differences, final Double alpha, final Double beta) {
        if (differences.length == 0) {
            return ImmutableMap.of();
        }

        final double[] sorted = Arrays.copyOf(differences, differences.length);
        Arrays.sort(sorted);

        final int size = differences.length;
        final double alphaFinal = Objects.requireNonNullElse(alpha, DEFAULT_VALUE);
        final double betaFinal = Objects.requireNonNullElse(beta, DEFAULT_VALUE);

        return Arrays.stream(QUANTILES)
            .boxed()
            .collect(Collectors.toMap(
                Function.identity(),
                quantile -> {
                    final double p = quantile;
                    final double m = alphaFinal + p * (1 - alphaFinal - betaFinal);
                    final double aleph = size * p + m;
                    final int k = (int) Math.floor(clip(aleph, 1, size - 1));
                    final double gamma = clip(aleph - k, 0, 1);
                    final double result = (1 - gamma) * sorted[k - 1] + gamma * sorted[k];
                    return QuantilePair.builder()
                        .quantile(result)
                        .formatted((double) Math.round(100 * result) / 100)
                        .build();
                }));
    }

    /**
     * Executes Apache's Common Math log beta function for provided alpha and beta values
     *
     * @param alpha alpha value
     * @param beta beta value
     * @return results from log beta function
     */
    private double logBeta(final double alpha, final double beta) {
        return LOG_BETA_FN.apply(alpha, beta);
    }

    /**
     * Validates passed {@link BayesianInput} instance to have the right parameters.
     *
     * @param input Bayesian calculation input
     */
    private void validateInput(final BayesianInput input) throws DotDataException {
        Objects.requireNonNull(input, "Bayesian input is missing");

        validateVariantPair(input.control());
        input.variantPairs().forEach(this::validateVariantPair);

        final List<VariantInputPair> variantPairs = new ArrayList<>(input.variantPairs());
        variantPairs.add(input.control());
        for (final VariantInputPair variantPair: variantPairs) {
            validateVariantsSize(input, input.type() == ABTestingType.ABC ? 2 : 1);
            if (variantPair.successes() + variantPair.failures() == 0) {
                throw new DotDataException("Variant successes and failures cannot be zero");
            }
        }

        if (Objects.nonNull(input.priors().alpha()) && input.priors().alpha() <= 0.0) {
            throw new IllegalArgumentException("Prior alpha cannot have a zero or negative value");
        }

        if (Objects.nonNull(input.priors().beta()) && input.priors().beta() <= 0.0) {
            throw new IllegalArgumentException("Prior beta cannot have a zero or negative value");
        }
    }

    private void validateVariantsSize(final BayesianInput input, final int expected) {
        if (input.variantPairs().size() != expected) {
            throw new IllegalArgumentException(String.format("AB test must have only %d variant", expected));
        }
    }

    /**
     * Validates passed {@link VariantInputPair} instance to have the right parameters.
     *
     * @param variantPair variant pair
     */
    private void validateVariantPair(final VariantInputPair variantPair) {
        if (variantPair.successes() < 0) {
            throw new IllegalArgumentException("Variant successes cannot have negative value");
        }

        if (variantPair.failures() < 0) {
            throw new IllegalArgumentException("Variant failures cannot have negative value");
        }
    }

    /**
     * Detects if the input has zero interactions and returns a NOOP result.
     *
     * @param input Bayesian calculation input
     * @return Optional with NOOP result if input has zero interactions, empty otherwise
     */
    private Optional<BayesianResult> detectNoop(final BayesianInput input) {
        try {
            validateInput(input);
            return Optional.empty();
        } catch (final DotDataException e) {
            Logger.error(
                this,
                String.format("Cannot calculate probability with zero interactions for input: %s", input), e);
            return Optional.of(NOOP_RESULT);
        }
    }

    /**
     * Evaluates is prior is present in the input.
     * @param input Bayesian calculation input
     * @return true if prior is present, false otherwise
     */
    private boolean isPriorPresent(final BayesianInput input) {
        return Objects.nonNull(input.priors().alpha()) && Objects.nonNull(input.priors().beta());
    }

}
