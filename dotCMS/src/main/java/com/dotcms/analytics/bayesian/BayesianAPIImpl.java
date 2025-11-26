package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.beta.BetaDistributionWrapper;
import com.dotcms.analytics.bayesian.model.*;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.metrics.timing.TimeMetric;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Bayesian calculation API.
 * So far it implements on method that calculates the probability for an A/B test that B (test) beats A (control).
 *
 * @author vico
 */
public class BayesianAPIImpl implements BayesianAPI, EventSubscriber<SystemTableUpdatedKeyEvent> {

    private final AtomicInteger betaDistSampleSize;
    private final AtomicBoolean includeBetaDistSamples;

    private static int resolveBetaDistSampleSize() {
        return Config.getIntProperty(BayesianAPI.BETA_DISTRIBUTION_SAMPLE_SIZE_KEY, 1000);
    }

    private static boolean resolveIncludeBetaDistSamples() {
        return Config.getBooleanProperty(BayesianAPI.INCLUDE_BETA_DISTRIBUTION_SAMPLES_KEY, false);
    }

    public BayesianAPIImpl() {
        betaDistSampleSize = new AtomicInteger(resolveBetaDistSampleSize());
        includeBetaDistSamples = new AtomicBoolean(resolveIncludeBetaDistSamples());
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianResult doBayesian(final BayesianInput input) {
        final TimeMetric timeMetric = TimeMetric.mark(getClass().getSimpleName());

        // validate input
        final BayesianResult bayesianResult = noopFallback(input)
            .orElseGet(() -> input.type() == ABTestingType.AB
                ? getAbBayesianResult(input)
                : getAbcBayesianResult(input));

        timeMetric.stop();

        return bayesianResult;
    }

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        if (event.getKey().contains(BETA_DISTRIBUTION_SAMPLE_SIZE_KEY)) {
            betaDistSampleSize.set(resolveBetaDistSampleSize());
        } else if (event.getKey().contains(INCLUDE_BETA_DISTRIBUTION_SAMPLES_KEY)) {
            includeBetaDistSamples.set(resolveIncludeBetaDistSamples());
        }
    }

    /**
     * Calculates probability that B (Test) beats A (Control) and B (Test) just as main.
     *
     * @param input Bayesian input
     * @return Bayesian result
     */
    private BayesianResult getAbBayesianResult(final BayesianInput input) {
        final VariantInput control = input.control();
        final VariantInput test = input.variantPairs().get(0);
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
        final DifferenceData differenceData = resolveDifference(() -> generateDifferenceData(controlBeta, testBeta));

        return BayesianResult.builder()
            .value(testGain)
            .results(List.of(
                toResult(
                    control,
                    controlConversionRate,
                    controlGain,
                    calcExpectedLoss(testConversionRate, controlConversionRate, controlGain),
                    calcRisk(controlBeta, testBeta),
                    calc95Credibility(priors, control),
                    null),
                toResult(
                    test,
                    testConversionRate,
                    testGain,
                    calcExpectedLoss(controlConversionRate, testConversionRate, testGain),
                    calcRisk(testBeta, controlBeta),
                    calc95Credibility(priors, test),
                    calcMedianGrowth(controlConversionRate, testConversionRate))))
            .suggestedWinner(suggestAbWinner(testGain, control.variant(), test.variant()))
            .distributionPdfs(
                resolveSampleGroup(
                    () -> calcDistributionsPdfs(control.variant(), controlBeta, test.variant(), testBeta),
                    List.of(control.variant(), test.variant())))
            .differenceData(differenceData)
            .quantiles(resolveQuantiles(() -> calcQuantiles(
                differenceData.differences(),
                priors.alpha(),
                priors.beta())))
            .build();
    }

    @VisibleForTesting
    void setBetaDistSampleSize(final int betaDistSampleSize) {
        this.betaDistSampleSize.set(betaDistSampleSize);
    }

    @VisibleForTesting
    void setIncludeBetaDistSamples(final boolean includeBetaDistSamples) {
        this.includeBetaDistSamples.set(includeBetaDistSamples);
    }

    /**
     * Calculates probability control and tests B and C.
     *
     * @param input Bayesian input
     * @return Bayesian result
     */
    private BayesianResult getAbcBayesianResult(final BayesianInput input) {
        final VariantInput control = input.control();
        final VariantInput testB = input.variantPairs().get(0);
        final VariantInput testC = input.variantPairs().get(1);
        final List<BayesianPriors> priors = Optional
            .ofNullable(input.priors())
            .filter(p -> p.size() == 3)
            .orElse(ImmutableList.of(
                BayesianAPI.DEFAULT_PRIORS,
                BayesianAPI.DEFAULT_PRIORS,
                BayesianAPI.DEFAULT_PRIORS));
        final BayesianPriors controlPriors = priors.get(0);
        final BayesianPriors testBPriors = priors.get(1);
        final BayesianPriors testCPriors = priors.get(2);
        final BetaDistributionWrapper controlBeta = BetaDistributionWrapper.create(
            controlPriors.alpha() + control.successes(),
            controlPriors.beta() + control.failures());
        final BetaDistributionWrapper testBBeta = BetaDistributionWrapper.create(
            testBPriors.alpha() + testB.successes(),
            testBPriors.beta() + testB.failures());
        final BetaDistributionWrapper testCBeta = BetaDistributionWrapper.create(
            testCPriors.alpha() + testC.successes(),
            testCPriors.beta() + testC.failures());
        final BayesianProcessingData controlData = BayesianProcessingData.builder()
            .input(control)
            .priors(priors.get(0))
            .distribution(controlBeta)
            .build();
        final BayesianProcessingData testBData = BayesianProcessingData.builder()
            .input(testB)
            .priors(testBPriors)
            .distribution(testBBeta)
            .build();
        final BayesianProcessingData testCData = BayesianProcessingData.builder()
            .input(testC)
            .priors(testCPriors)
            .distribution(testCBeta)
            .build();
        final List<VariantResult> results = enrichResults(
            calcAbcTesting(controlData, testBData, testCData),
            controlData,
            testBData,
            testCData);

        return BayesianResult.builder()
            .value(results.get(0).probability())
            .results(results)
            .suggestedWinner(suggestAbcWinner(results))
            .distributionPdfs(
                resolveSampleGroup(() ->
                    calcDistributionsPdfs(
                        ImmutableMap.of(
                            control.variant(), controlBeta,
                            testB.variant(), testBBeta,
                            testC.variant(), testCBeta)),
                        List.of(control.variant(), testB.variant(), testC.variant())))
            .build();
    }

    /**
     * Resolves difference data.
     *
     * @param differenceDataSupplier supplier of difference data
     * @return difference data
     */
    private DifferenceData resolveDifference(final Supplier<DifferenceData> differenceDataSupplier) {
        return includeBetaSamples()
                ? differenceDataSupplier.get()
                : EMPTY_DIFFERENCE;
    }

    /**
     * Resolves sample group.
     *
     * @param sampleGroupSupplier supplier of sample group
     * @param variants variants
     * @return sample group
     */
    private SampleGroup resolveSampleGroup(final Supplier<SampleGroup> sampleGroupSupplier,
                                           final List<String> variants) {
        return includeBetaSamples()
                ? sampleGroupSupplier.get()
                : SampleGroup.builder()
                    .samples(variants
                        .stream()
                        .collect(Collectors.toMap(Function.identity(), variant -> Collections.emptyList())))
                    .build();
    }

    /**
     * Calculates quantiles for a provided difference data.
     *
     * @param quantilesSupplier supplier of quantiles
     * @return map of quantiles
     */
    private Map<Double, QuantilePair> resolveQuantiles(final Supplier<Map<Double, QuantilePair>> quantilesSupplier) {
        return includeBetaSamples()
                ? quantilesSupplier.get()
                : Collections.emptyMap();
    }

    /**
     * Calculates conversion rate for a provided variant.
     *
     * @param input variant input
     * @return conversion rate
     */
    private double calcConversionRate(final VariantInput input) {
        return (double) input.successes() / (input.successes() + input.failures());
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
     *         for i = 0:(α_B - 1)
     *             total += exp(logbeta(α_A + i, β_B + β_A)
     *                 - log(β_B + i) - logbeta(1 + i, β_B) - logbeta(α_A, β_A))
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
                                          final VariantInput control,
                                          final VariantInput test) {
        return calcProbabilityBBeatsA(
            control.successes() + priors.alpha(),
            control.failures() + priors.beta(),
            test.successes() + priors.alpha(),
            test.failures() + priors.beta());
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) based on this pseudo (Julia) code:
     *
     * <pre>
     *    function probability_C_beats_A_and_B(α_A, β_A, α_B, β_B, α_C, β_C)
     *        total = 0.0
     *        for i = 0:(α_A-1)
     *            for j = 0:(α_B-1)
     *                total += exp(logbeta(α_C + i + j, β_A + β_B + β_C) - log(β_A + i) - log(β_B + j)
     *                    - logbeta(1 + i, β_A) - logbeta(1 + j, β_B) - logbeta(α_C, β_C))
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
    private double calcProbabilityCBeatsA_B(final double controlSuccesses,
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

        final double probAOverC = calcProbabilityBBeatsA(
            testCSuccesses,
            testCFailures,
            controlSuccesses,
            controlFailures);
        final double probBOverC = calcProbabilityBBeatsA(testCSuccesses, testCFailures, testBSuccesses, testBFailures);

        return 1 - probAOverC - probBOverC + total;
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test)
     *
     * @param controlData control data
     * @param testBData test B data
     * @param testCData test C data
     * @param finalControl final control
     */
    private VariantResult calcSingleAbcTesting(final BayesianProcessingData controlData,
                                               final BayesianProcessingData testBData,
                                               final BayesianProcessingData testCData,
                                               final VariantInput finalControl) {
        final VariantInput control = controlData.input();
        final BayesianPriors controlPriors = controlData.priors();
        final VariantInput testB = testBData.input();
        final BayesianPriors testBPriors = testBData.priors();
        final VariantInput testC = testCData.input();
        final BayesianPriors testCPriors = testCData.priors();
        final double controlConversionDate = calcConversionRate(finalControl);
        final double testConversionRate = calcConversionRate(testC);
        return toResult(
            testC,
            calcConversionRate(testC),
            calcProbabilityCBeatsA_B(
                control.successes() + controlPriors.alpha(),
                control.failures() + controlPriors.beta(),
                testB.successes() + testBPriors.alpha(),
                testB.failures() + testBPriors.beta(),
                testC.successes() + testCPriors.alpha(),
                testC.failures() + testCPriors.beta()),
            null,
            null,
            calc95Credibility(testCPriors, testC),
            testC == finalControl ? null : calcMedianGrowth(controlConversionDate, testConversionRate));
    }

    /**
     * Calculates probability that C (Test) beats A (Control) and B (Test) just as main
     * {@link #doBayesian(BayesianInput)}
     *
     * @param controlData control successes/failures and priors
     * @param testBData test B successes/failures and priors
     * @param testCData test C successes/failures and priors
     * @return a list {@link VariantResult} representing probability that C beats A and B
     */
    private List<VariantResult> calcAbcTesting(final BayesianProcessingData controlData,
                                               final BayesianProcessingData testBData,
                                               final BayesianProcessingData testCData) {
        final Optional<VariantInput> finalControl = Stream
            .of(controlData, testBData, testCData)
            .filter(data -> data.input().isControl())
            .map(BayesianProcessingData::input)
            .findFirst();
        return Stream
            .of(
                List.of(testCData, testBData, controlData),
                List.of(controlData, testCData, testBData),
                List.of(testBData, controlData, testCData))
            .map(data -> calcSingleAbcTesting(data.get(0), data.get(1), data.get(2), finalControl.orElse(null)))
            .collect(Collectors.toList());
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
    private double[] calc95Credibility(final BayesianPriors priors, final VariantInput input) {
        return calc95Credibility(priors.alpha(), priors.beta(), input.successes(), input.failures());
    }

    /**
     * Calculate expected loss for the given input.
     *
     * @param conversionRate the conversion rate
     * @return the loss
     */
    private double calcLoss(final double conversionRate) {
        return 1 - conversionRate;
    }

    /**
     * Calcualtes expected loss for the given input.
     *
     * @param controlConversionRate the control conversion rate
     * @param testConversionRate the test conversion rate
     * @param probabilityBBeatsA the probability that the test variant beats the control variant
     * @return the expected loss
     */
    private double calcExpectedLoss(final double controlConversionRate,
                                    final double testConversionRate,
                                    final double probabilityBBeatsA) {
        // Calculate the expected loss
        return probabilityBBeatsA
            * calcLoss(controlConversionRate)
            + calcLoss(probabilityBBeatsA)
            * calcLoss(testConversionRate);
    }

    /**
     * Calculates expected losses for the given inputs.
     *
     * @param controlResult the control result
     * @param testBResult the test B result
     * @param testCResult the test C result
     * @return the expected losses
     */
    private Map<String, Double> calcExpectedLoss(final VariantResult controlResult,
                                                 final VariantResult testBResult,
                                                 final VariantResult testCResult) {
        return Map.of(
            controlResult.variant(),
            calcExpectedLoss(testBResult.probability(), testCResult.probability(), controlResult.conversionRate()),
            testBResult.variant(),
            calcExpectedLoss(1 - testBResult.probability(), testCResult.probability(), testBResult.conversionRate()),
            testCResult.variant(),
            calcExpectedLoss(
                1 - testCResult.probability(),
                1 - testBResult.probability(),
                testCResult.conversionRate()));
    }

    /**
     * Enriches results with expected losses.
     *
     * @param results results
     * @return enriched results
     */
    private List<VariantResult> enrichResults(final List<VariantResult> results, final BayesianProcessingData controlData, final BayesianProcessingData testBData, final BayesianProcessingData testCData) {
        final Map<String, Double> expectedLosses = calcExpectedLoss(results.get(0), results.get(1), results.get(2));
        final Map<String, Double> risks = calcRisk(
            controlData,
            results.get(0).conversionRate(),
            testBData,
            results.get(1).conversionRate(),
            testCData,
            results.get(2).conversionRate());

        return results
            .stream()
            .map(result -> result
                .withExpectedLoss(expectedLosses.get(result.variant()))
                .withRisk(risks.get(result.variant())))
            .collect(Collectors.toList());
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
     * @param controlData control variant data
     * @param controlConversionRate control variant conversion rate
     * @param testBData test B variant data
     * @param testBConversionRate test B variant conversion rate
     * @param testCData test C variant data
     * @param testCConversionRate test C variant conversion rate
     * @return risk of the test variant beating the control variant
     */
    private Map<String, Double> calcRisk(final BayesianProcessingData controlData,
                                         final double controlConversionRate,
                                         final BayesianProcessingData testBData,
                                         final double testBConversionRate,
                                         final BayesianProcessingData testCData,
                                         final double testCConversionRate) {
        final double testBPosterior = testBData.distribution().inverseCumulativeProbability(controlConversionRate);
        final double testCPosterior = testCData.distribution().inverseCumulativeProbability(controlConversionRate);
        final double relativeRiskB =
            testBPosterior / controlData.distribution().inverseCumulativeProbability(testBConversionRate);
        final double relativeRiskC =
            testCPosterior / controlData.distribution().inverseCumulativeProbability(testCConversionRate);

        /*return Map.of(
            testBData.input().variant(), relativeRiskB,
            testCData.input().variant(), relativeRiskC);*/

        double controlProbability = controlData.distribution().rv();
        double testBProbability = testBData.distribution().rv();
        double testCProbability = testCData.distribution().rv();
        double testBRisk = testBProbability / controlProbability;
        double testCRisk = testCProbability / controlProbability;

        return Map.of(
            testBData.input().variant(), testBRisk,
            testCData.input().variant(), testCRisk);
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
     * Given a map of {@link BetaDistributionWrapper} instances calculates density (pdf) elements for each one.
     *
     * @param distributions provided beta distributions
     * @return {@link SampleGroup} containing each sample list against an identifier
     */
    private SampleGroup calcDistributionsPdfs(final Map<String, BetaDistributionWrapper> distributions) {
        return SampleGroup.builder()
                .samples(distributions
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> calcPdfElements(entry.getValue()))))
                .build();
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
        return calcDistributionsPdfs(ImmutableMap.of(
                control, controlDistribution,
                test, testDistribution
        ));
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

    private boolean includeBetaSamples() {
        return includeBetaDistSamples.get();
    }

    /**
     * Resolves sample size for beta distribution.
     *
     * @return sample size
     */
    private int resolveSampleSize() {
        return betaDistSampleSize.get();
    }

    /**
     * Converts {@link VariantInput} including probability and credibility interval to
     * {@link VariantResult}.
     *
     * @param input variant input
     * @param conversionRate conversion rate
     * @param probability probability that B (Test) beats A (Control)
     * @param risk risk
     * @param credibilityInterval credibility interval
     * @param medianGrowth median growth
     * @return {@link VariantResult} instance
     */
    private VariantResult toResult(final VariantInput input,
                                   final double conversionRate,
                                   final double probability,
                                   final Double expectedLoss,
                                   final Double risk,
                                   final double[] credibilityInterval,
                                   final Double medianGrowth) {
        return VariantResult.builder()
            .variant(input.variant())
            .isControl(input.isControl())
            .conversionRate(conversionRate)
            .probability(probability)
            .expectedLoss(expectedLoss)
            .risk(risk)
            .medianGrowth(medianGrowth)
            .credibilityInterval(
                CredibilityInterval.builder()
                    .lower(credibilityInterval[0])
                    .upper(credibilityInterval[1])
                    .build())
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
     * Validates passed {@link VariantInput} instance to have the right parameters.
     *
     * @param variantPair variant pair
     */
    private void validateVariantPair(final VariantInput variantPair) {
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

        final List<VariantInput> variantPairs = new ArrayList<>(input.variantPairs());
        variantPairs.add(input.control());
        for (final VariantInput variantPair: variantPairs) {
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
            Logger.debug(
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
    private Map<Double, QuantilePair> calcQuantiles(final double[] differences, final double alpha, final double beta) {
        if (differences.length == 0) {
            return ImmutableMap.of();
        }

        final double[] sorted = Arrays.copyOf(differences, differences.length);
        Arrays.sort(sorted);

        final int size = differences.length;

        return Arrays.stream(QUANTILES)
            .boxed()
            .collect(Collectors.toMap(
                Function.identity(),
                quantile -> {
                    final double p = quantile;
                    final double m = alpha + p * (1 - alpha - beta);
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
