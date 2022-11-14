package com.dotcms.analytics.bayesian;


import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.bayesian.model.DifferenceData;
import com.dotcms.analytics.bayesian.model.QuantilePair;
import com.dotcms.analytics.bayesian.model.SampleData;
import com.dotcms.analytics.bayesian.model.SampleGroup;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.numbers.gamma.LogBeta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
    private static final BiFunction<Integer, Integer, Double> LOG_BETA_FN = LogBeta::value;

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianResult calcABTesting(final BayesianInput input) {
        // validate input
        validateInput(input);

        final BetaDistribution controlData = new BetaModel(input).distribution();
        final BetaDistribution testData = new BetaModel(input).distribution();
        final DifferenceData differenceData = generateDifferenceData(controlData, testData);

        // call calculation
        return BayesianResult.builder()
                .result(calcABTesting(
                        input.controlSuccesses() + 1,
                        input.controlFailures() + 1,
                        input.testSuccesses() + 1,
                        input.testFailures() + 1))
                .distributionPdfs(calcDistributionsPdfs(controlData, testData))
                .differenceData(differenceData)
                .quantiles(calcQuantiles(differenceData.differences(), null, null))
                .build();
    }

    /**
     * Calculates probability that B (Test) beats A (Control) just as main {@link #calcABTesting(BayesianInput)}
     *
     * @param alphaA control successes
     * @param betaA control failures
     * @param alphaB test successes
     * @param betaB test failures
     * @return result representing probability that B beats A
     */
    private double calcABTesting(final int alphaA, final int betaA, final int alphaB, final int betaB) {
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
     * Given a {@link BetaDistribution} instance calculates density (pdf) elements.
     *
     * @param distribution provided beta distribution
     * @return list of {@link SampleData} instances
     */
    private List<SampleData> calcPdfElements(final BetaDistribution distribution) {
        return IntStream
                .range(0, SAMPLE_SIZE)
                .mapToObj(operand -> {
                    final double x = (double) operand / SAMPLE_SIZE;
                    final double temp = distribution.pdf(x);
                    final double y = temp == Double.POSITIVE_INFINITY ? 0 : temp;
                    return SampleData.builder()
                            .x(x)
                            .y(y)
                            .build();
                })
                .collect(Collectors.toList());
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
        final double[] controlData = controlDistribution.rvs(SAMPLE_SIZE);
        final double[] testData = testDistribution.rvs(SAMPLE_SIZE);
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
     * @return list of calculated quantiles double values
     */
    private Map<Double, QuantilePair> calcQuantiles(final double[] differences, final Double alpha, final Double beta) {
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
    private double logBeta(final int alpha, final int beta) {
        return LOG_BETA_FN.apply(alpha, beta);
    }

    /**
     * Validates passed {@link BayesianInput} instance to have the right parameters.
     *
     * @param input Bayesian calculation input
     */
    private void validateInput(final BayesianInput input) {
        Objects.requireNonNull(input, "Bayesian input is missing");
        if (input.priorAlpha() <= 0.0) {
            throw new IllegalArgumentException("Prior alpha cannot have a zero or negative value");
        }

        if (input.priorBeta() <= 0.0) {
            throw new IllegalArgumentException("Prior beta cannot have a zero or negative value");
        }

        if (input.controlSuccesses() < 0) {
            throw new IllegalArgumentException("Control successes cannot have negative value");
        }

        if (input.controlFailures() < 0) {
            throw new IllegalArgumentException("Control failures cannot have negative value");
        }

        if (input.testSuccesses() < 0) {
            throw new IllegalArgumentException("Test successes cannot have negative value");
        }

        if (input.testFailures() < 0) {
            throw new IllegalArgumentException("Test failures cannot have negative value");
        }
    }

}
