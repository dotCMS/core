package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.beta.BetaDistributionWrapper;
import com.dotcms.analytics.bayesian.model.ABTestingType;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.bayesian.model.CredibilityInterval;
import com.dotcms.analytics.bayesian.model.DifferenceData;
import com.dotcms.analytics.bayesian.model.QuantilePair;
import com.dotcms.analytics.bayesian.model.SampleData;
import com.dotcms.analytics.bayesian.model.SampleGroup;
import com.dotcms.analytics.bayesian.model.VariantBayesianInput;
import com.dotcms.analytics.bayesian.model.VariantResult;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private final Lazy<Integer> betaDistSamples =
            Lazy.of(() -> Config.getIntProperty("BETA_DISTRIBUTION_SAMPLE_SIZE", 1000));

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianResult doBayesian(final BayesianInput input) {
        // validate input
        return noopFallback(input)
            .orElseGet(() -> input.type() == ABTestingType.AB
                ? getAbBayesianResult(input)
                : getAbcBayesianResult(input));
    }

    /**
     * Calculates probability that B (Test) beats A (Control) and B (Test) just as main.
     *
     * @param input Bayesian input
     * @return Bayesian result
     */
    private BayesianResult getAbBayesianResult(final BayesianInput input) {
        final VariantBayesianInput control = input.control();
        final VariantBayesianInput test = input.variantPairs().get(0);
        final BayesianPriors priors = input.priors().get(0);

        final double testGain = calcProbabilityBBeatsA(priors, control, test);
        final double controlGain = 1.0 - testGain;
        final double controlConversionRate = calcConversionRate(control);
        final double testConversionRate = calcConversionRate(test);
        final BetaDistributionWrapper controlBeta = BetaDistributionWrapper.create(
            priors.alpha() + control.successes(),
            priors.beta() + control.failures());
        final BetaDistributionWrapper testBeta = BetaDistributionWrapper.create(
            priors.alpha() + test.successes(),
            priors.beta() + test.failures());
        final DifferenceData differenceData = generateDifferenceData(controlBeta, testBeta);

        return BayesianResult.builder()
            .value(testGain)
            .results(List.of(
                toResult(
                    control,
                    controlGain,
                    calc95Credibility(priors, control),
                    controlConversionRate,
                    null,
                    calcRisk(controlBeta, testBeta)),
                toResult(
                    test,
                    testGain,
                    calc95Credibility(priors, test),
                    testConversionRate,
                    calcMedianGrowth(controlConversionRate, testConversionRate),
                    calcRisk(testBeta, controlBeta))))
            .suggestedWinner(suggestAbWinner(testGain, control.variant(), test.variant()))
            .distributionPdfs(calcDistributionsPdfs(control.variant(), controlBeta, test.variant(), testBeta))
            .differenceData(differenceData)
            .quantiles(calcQuantiles(differenceData.differences(), priors.alpha(), priors.beta()))
            .build();
    }

    /**
     * Calculates conversion rate for a provided variant.
     *
     * @param input variant input
     * @return conversion rate
     */
    private double calcConversionRate(final VariantBayesianInput input) {
        return (double) input.successes() / (input.successes() + input.failures());
    }

    /**
     * Calculates test conversion rate growth against control's.
     *
     * @param controlConversionRate control conversion rate
     * @param testConversionRate test conversion rate
     * @return test conversion rate growth against control's
     */
    private double calcMedianGrowth(final double controlConversionRate, final double testConversionRate) {
        return (testConversionRate - controlConversionRate) / testConversionRate;
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
     * {@link BetaDistributionWrapper} which provides en density function {@see DotBetaDistribution.pdf()}.
     *
     * @param controlSuccesses number of successes for control
     * @param controlFailures number of failures for control
     * @param testSuccesses number of successes for test
     * @param testFailures number of failures for test
     */
    private double calcProbabilityBBeatsA(final double controlSuccesses,
                                          final double controlFailures,
                                          final double testSuccesses,
                                          final double testFailures) {
        double result = 0.0;
        for(int i = 0; i < testSuccesses; i++) {
            result += Math.exp(
                logBeta(controlSuccesses + i, testFailures + controlFailures)
                    - Math.log(testFailures + i)
                    - logBeta(1 + i, testFailures)
                    - logBeta(controlSuccesses, controlFailures));
        }
        return result;
    }

    /**
     * Calculates probability that B (Test) beats A (Control)
     *
     * @param priors the priors
     * @param control the control
     * @param test the test
     */
    private double calcProbabilityBBeatsA(final BayesianPriors priors,
                                          final VariantBayesianInput control,
                                          final VariantBayesianInput test) {
        return calcProbabilityBBeatsA(
            control.successes() + priors.alpha(),
            control.failures() + priors.beta(),
            test.successes() + priors.alpha(),
            test.failures() + priors.beta());
    }

    /**
     * Calculates probability control and tests B and C.
     *
     * @param input Bayesian input
     * @return Bayesian result
     */
    private BayesianResult getAbcBayesianResult(final BayesianInput input) {
        final List<VariantResult> results = calcAbcProbabilities(
            Pair.create(input.priors().get(0), input.control()),
            Pair.create(input.priors().get(1), input.variantPairs().get(0)),
            Pair.create(input.priors().get(2), input.variantPairs().get(1)));

        return BayesianResult.builder()
            .value(results.get(0).probability())
            .results(results)
            .suggestedWinner(suggestAbcWinner(results))
            .build();
    }

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
     * {@link BetaDistributionWrapper} which provides en density function {@see DotBetaDistribution.pdf()}.
     *
     * @param controlSuccesses control successes
     * @param controlFailures control failures
     * @param testBSuccesses test B successes
     * @param testBFailures test B failures
     * @param testCSuccesses test C successes
     * @param testCFailures test C failures
     * @return probability that C (Test) beats A (Control) and B (Test)
     */
    private double calcAbcProbabilities(final double controlSuccesses,
                                        final double controlFailures,
                                        final double testBSuccesses,
                                        final double testBFailures,
                                        final double testCSuccesses,
                                        final double testCFailures) {
        double total = 0.0;

        for(int i = 0; i < controlSuccesses; i++) {
            for(int j = 0; j < testBSuccesses; j++) {
                total += Math.exp(
                    logBeta(testCSuccesses + i + j, controlFailures + testBFailures + testCFailures)
                        - Math.log(controlFailures + i)
                        - Math.log(testBFailures + j)
                        - logBeta(1 + i, controlFailures)
                        - logBeta(1 + j, testBFailures)
                        - logBeta(testCSuccesses, testCFailures));
            }
        }

        final double probAOverC = calcProbabilityBBeatsA(testCSuccesses, testCFailures, controlSuccesses, controlFailures);
        final double probBOverC = calcProbabilityBBeatsA(testCSuccesses, testCFailures, testBSuccesses, testBFailures);

        return 1 - probAOverC - probBOverC + total;
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) just as main
     * {@link #doBayesian(BayesianInput)}
     *
     * @param controlPriors control alpha and beta
     * @param control successes and failures
     * @param testBPriors test B alpha and beta
     * @param testB successes and failures
     * @param testCPriors test C alpha and beta
     * @param testC successes and failures
     * @return a list {@link VariantResult} representing probability that C beats A and B
     */
    private VariantResult calcSingleAbcTesting(final BayesianPriors controlPriors,
                                               final VariantBayesianInput control,
                                               final BayesianPriors testBPriors,
                                               final VariantBayesianInput testB,
                                               final BayesianPriors testCPriors,
                                               final VariantBayesianInput testC) {
        return toResult(
            testC,
            calcAbcProbabilities(
                control.successes() + controlPriors.alpha(),
                control.failures() + controlPriors.beta(),
                testB.successes() + testBPriors.alpha(),
                testB.failures() + testBPriors.beta(),
                testC.successes() + testCPriors.alpha(),
                testC.failures() + testCPriors.beta()),
            calc95Credibility(testCPriors, testC),
            0.0,
            0.0,
            0.0);
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) just as main
     * {@link #doBayesian(BayesianInput)}
     *
     * @param controlPair control successes/failures and priors
     * @param testBPair test B successes/failures and priors
     * @param testCPair test C successes/failures and priors
     * @return a list {@link VariantResult} representing probability that C beats A and B
     */
    private List<VariantResult> calcAbcProbabilities(final Pair<BayesianPriors, VariantBayesianInput> controlPair,
                                                     final Pair<BayesianPriors, VariantBayesianInput> testBPair,
                                                     final Pair<BayesianPriors, VariantBayesianInput> testCPair) {
        return Stream
            .of(
                List.of(testCPair, testBPair, controlPair),
                List.of(controlPair, testCPair, testBPair),
                List.of(testBPair, controlPair, testCPair))
            .map(inputs -> calcSingleAbcTesting(
                inputs.get(0).getFirst(), inputs.get(0).getSecond(),
                inputs.get(1).getFirst(), inputs.get(1).getSecond(),
                inputs.get(2).getFirst(), inputs.get(2).getSecond()))
            .collect(Collectors.toList());
    }

    /**
     * Calculates the 95% credibility interval for the given input.
     *
     * @param priorAlpha the prior alpha
     * @param priorBeta the prior beta
     * @param successes the successes
     * @param failures the failures
     * @return the 95% credibility interval
     */
    private double[] calc95Credibility(final double priorAlpha,
                                       final double priorBeta,
                                       final long successes,
                                       final long failures) {
        final BetaDistribution betaDistA = new BetaDistribution(priorAlpha + successes, priorBeta + failures);
        final double lowerBound = betaDistA.inverseCumulativeProbability(LOWER_BOUND_PROBABILITY);
        final double upperBound = betaDistA.inverseCumulativeProbability(UPPER_BOUND_PROBABILITY);
        return new double[] { lowerBound, upperBound };
    }

    /**
     * Calculates the 95% credibility interval for the given input.
     *
     * @param priors the priors
     * @param input the input
     * @return the 95% credibility interval
     */
    private double[] calc95Credibility(final BayesianPriors priors, final VariantBayesianInput input) {
        return calc95Credibility(priors.alpha(), priors.beta(), input.successes(), input.failures());
    }

    /**
     * Calculates the mean of the beta distribution.
     *
     * @param priorAlpha the prior alpha
     * @param priorBeta the prior beta
     * @param successes the number of successes
     * @param failures the number of failures
     * @return the mean
     */
    private double calcMean(final double priorAlpha,
                            final double priorBeta,
                            final long successes,
                            final long failures) {
        return (priorAlpha + successes) / (priorAlpha + successes + priorBeta + failures);
    }

    /**
     * Calculates variance for the beta distribution.
     *
     * @param priorAlpha the prior alpha
     * @param priorBeta the prior beta
     * @param successes the number of successes
     * @param failures the number of failures
     * @return the variance
     */
    private double calcVariance(final double priorAlpha,
                                final double priorBeta,
                                final long successes,
                                final long failures) {
        return (successes + priorAlpha)
            * (failures + priorBeta)
            / (FastMath.pow(priorAlpha + successes + priorBeta + failures, 2)
            * (priorAlpha + successes + priorBeta + failures + 1));
    }

    /**
     * Calculates the risk of the test variant beating the control variant.
     *
     * @param priorAlpha the prior alpha
     * @param priorBeta the prior beta
     * @param controlSuccesses the number of successes for the control variant
     * @param controlFailures the number of failures for the control variant
     * @param testSuccesses the number of successes for the test variant
     * @param testFailures the number of failures for the test variant
     * @return the risk of the test variant beating the control variant
     */
    private double calcRiskCheap(final double priorAlpha,
                                 final double priorBeta,
                                 final long controlSuccesses,
                                 final long controlFailures,
                                 final long testSuccesses,
                                 final long testFailures) {
        // CLT approximation for Variants A and B
        final double meanA = calcMean(priorAlpha, priorBeta, controlSuccesses, controlFailures);
        final double varianceA = calcMean(priorAlpha, priorBeta, controlSuccesses, controlFailures);
        final double meanB = calcMean(priorAlpha, priorBeta, testSuccesses, testFailures);
        final double varianceB = calcVariance(priorAlpha, priorBeta, testSuccesses, testFailures);
        final double meanDifference = meanA - meanB;
        final double varianceDifference = varianceA + varianceB;

        // Defining the function to be integrated
        UnivariateFunction function = x -> {
            double pdf = FastMath.exp(-FastMath.pow(x - meanDifference, 2) / (2 * varianceDifference))
                / FastMath.sqrt(2 * FastMath.PI * varianceDifference);
            return x * pdf;
        };

        // Gaussian quadrature integration
        final UnivariateIntegrator integrator = new SimpsonIntegrator(); // or any other integrator
        return integrator.integrate(1000, function, -15, 15); // adjust parameters as needed
    }

    /**
     * Calculates the risk of the test variant beating the control variant.
     *
     * @param controlBeta control variant beta distribution
     * @param testBeta test variant beta distribution
     * @return risk of the test variant beating the control variant
     */
    private double calcRisk(final BetaDistributionWrapper controlBeta, final BetaDistributionWrapper testBeta) {
        final int samples = resolveSampleSize();
        final SummaryStatistics stats = new SummaryStatistics();

        IntStream.range(0, samples).forEach(i -> stats.addValue(controlBeta.rv() - testBeta.rv()));

        return stats.getMean();
    }

    /**
     * Calculates the risk of the test variant beating the control variant.
     *
     * @param priors Bayesian priors
     * @param controlInput control variant input
     * @param testInput test variant input
     * @return risk of the test variant beating the control variant
     */
    private double calcRisk(final BayesianPriors priors,
                            final VariantBayesianInput controlInput,
                            final VariantBayesianInput testInput) {
        return calcRiskCheap(
            priors.alpha(),
            priors.beta(),
            controlInput.successes(),
            controlInput.failures(),
            testInput.successes(),
            testInput.failures());
    }

    /**
     * Resolves which variant could be considered as the winner of the test.
     *
     * @param value calculated probability that B (Test) beats A (Control)
     * @param control control variant
     * @param test test variant
     * @return winner variant name
     */
    private String suggestAbWinner(final double value, final String control, final String test) {
        if (Double.compare(HALF, value) == 0 || Math.abs(HALF - value) <= TIE_COMPARE_DELTA) {
            return TIE;
        }

        return value < HALF ? control : test;
    }

    /**
     * Resolves which variant could be considered as the winner of the test.
     *
     * @param results list of probabilities
     * @return winner variant name
     */
    private String suggestAbcWinner(final List<VariantResult> results) {
        final VariantResult controlResult = results.get(0);
        if (results
            .stream()
            .allMatch(result -> result.probability() == controlResult.probability())) {
            return TIE;
        }

        return results
            .stream()
            .max(VARIANT_RESULT_COMPARATOR)
            .map(VariantResult::variant)
            .orElse(NONE);
    }

    /**
     * Given a {@link BetaDistributionWrapper} instance calculates density (pdf) elements.
     *
     * @param distribution provided beta distribution
     * @return list of {@link SampleData} instances
     */
    private List<SampleData> calcPdfElements(final BetaDistributionWrapper distribution) {
        final int sampleSize = resolveSampleSize();
        return IntStream
            .range(0, sampleSize)
            .mapToObj(operand -> {
                final double x = (double) operand / sampleSize;
                final double val = distribution.pdf(x);
                final double y = val == Double.POSITIVE_INFINITY ? 0 : val;
                return SampleData.builder()
                    .x(x)
                    .y(y)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Given a couple {@link BetaDistributionWrapper} objects calculates density(PDF) for both control (A) and test (B).
     *
     * @param controlDistribution provided control beta distribution
     * @param testDistribution provided test beta distribution
     * @return {@link SampleGroup} containing each sample list against an identifier
     */
    private SampleGroup calcDistributionsPdfs(final String control,
                                              final BetaDistributionWrapper controlDistribution,
                                              final String test,
                                              final BetaDistributionWrapper testDistribution) {
        return SampleGroup.builder()
            .samples(ImmutableMap.of(
                control, calcPdfElements(controlDistribution),
                test, calcPdfElements(testDistribution)))
            .build();
    }

    /**
     * Given a couple {@link BetaDistributionWrapper} objects generates difference between test and control sample data.
     *
     * @param controlDistribution provided control beta distribution
     * @param testDistribution provided test beta distribution
     * @return {@link  DifferenceData} instance with control and test data and its difference
     */
    private DifferenceData generateDifferenceData(final BetaDistributionWrapper controlDistribution,
                                                  final BetaDistributionWrapper testDistribution) {
        final int sampleSize = resolveSampleSize();
        final double[] controlData = controlDistribution.rvs(sampleSize);
        final double[] testData = testDistribution.rvs(sampleSize);
        final double[] differences = IntStream.range(0, controlData.length)
            .mapToDouble(i -> testData[i] - controlData[i])
            .toArray();
        return DifferenceData.builder()
            .controlData(controlData)
            .testData(testData)
            .differences(differences)
            .relativeDifference(Arrays.stream(differences).average().orElse(0.0))
            .build();
    }

    /**
     * Resolves sample size for beta distribution.
     *
     * @return sample size
     */
    private int resolveSampleSize() {
        return betaDistSamples.get();
    }

    /**
     * Converts {@link VariantBayesianInput} including probability, credibility interval, and risk to
     * {@link VariantResult}.
     *
     * @param input variant input
     * @param probability probability that B (Test) beats A (Control)
     * @param credibilityInterval credibility interval
     * @param conversionRate conversion rate
     * @param medianGrowth median growth
     * @param risk risk
     * @return {@link VariantResult} instance
     */
    private VariantResult toResult(final VariantBayesianInput input,
                                   final double probability,
                                   final double[] credibilityInterval,
                                   final double conversionRate,
                                   final Double medianGrowth,
                                   final double risk) {
        return VariantResult.builder()
            .variant(input.variant())
            .probability(probability)
            .conversionRate(conversionRate)
            .medianGrowth(medianGrowth)
            .credibilityInterval(
                CredibilityInterval.builder()
                    .lower(credibilityInterval[0])
                    .upper(credibilityInterval[1])
                    .build())
            .risk(risk)
            .build();
    }

    /**
     * Validates passed {@link BayesianInput} instance to have the right parameters.
     *
     * @param input Bayesian calculation input
     * @param expected expected number of variants
     */
    private void validateVariantsSize(final BayesianInput input, final int expected) {
        if (input.variantPairs().size() != expected) {
            throw new IllegalArgumentException(String.format("AB test must have only %d variant", expected));
        }
    }

    /**
     * Validates passed {@link VariantBayesianInput} instance to have the right parameters.
     *
     * @param variantPair variant pair
     */
    private void validateVariantPair(final VariantBayesianInput variantPair) {
        if (variantPair.successes() < 0) {
            throw new IllegalArgumentException("Variant successes cannot have negative value");
        }
        if (variantPair.failures() < 0) {
            throw new IllegalArgumentException("Variant failures cannot have negative value");
        }
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

        final List<VariantBayesianInput> variantPairs = new ArrayList<>(input.variantPairs());
        variantPairs.add(input.control());
        for (final VariantBayesianInput variantPair: variantPairs) {
            validateVariantsSize(input, input.type() == ABTestingType.ABC ? 2 : 1);
            if (variantPair.successes() + variantPair.failures() == 0) {
                throw new DotDataException("Variant successes and failures cannot be zero");
            }
        }

        for(final BayesianPriors priors: input.priors()) {
            if (priors.alpha() <= 0.0) {
                throw new IllegalArgumentException("Prior alpha cannot have a zero or negative value");
            }
            if (priors.beta() <= 0.0) {
                throw new IllegalArgumentException("Prior beta cannot have a zero or negative value");
            }
        }
    }

    /**
     * Detects if the input has zero interactions and returns a NOOP result.
     *
     * @param input Bayesian calculation input
     * @return Optional with NOOP result if input has zero interactions, empty otherwise
     */
    private Optional<BayesianResult> noopFallback(final BayesianInput input) {
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
        final double alphaFinal = Objects.requireNonNullElse(alpha, BETA_DIST_DEFAULT);
        final double betaFinal = Objects.requireNonNullElse(beta, BETA_DIST_DEFAULT);

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

}
