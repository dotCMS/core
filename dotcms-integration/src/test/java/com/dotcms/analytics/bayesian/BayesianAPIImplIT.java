package com.dotcms.analytics.bayesian;

import com.dotcms.IntegrationTestBase;
import com.dotcms.analytics.bayesian.model.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Bayesian API implementation class unit tests.
 *
 * @author vico
 */
public class BayesianAPIImplIT extends IntegrationTestBase {

    private static BayesianAPI bayesianAPI;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        bayesianAPI = APILocator.getBayesianAPI();
        ((BayesianAPIImpl) bayesianAPI).setIncludeBetaDistSamples(true);
    }

    /**
     * Given Bayesian input parameters with these values:
     *
     * <pre>
     *     priorAlpha: 1.0
     *     priorBeta: 1.0
     *     controlSuccesses: 100
     *     controlFailures: 900
     *     testSuccesses: 130
     *     testFailures: 870
     * </pre>
     *
     * Expect that the probability B beats A is at least 0.89.
     */
    @Test
    public void test_doBayesian_AB() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(100).failures(900).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(130).failures(870).isControl(false).build())
            .priors(List.of(BayesianAPI.DEFAULT_PRIORS))
            .build();
        final BayesianResult result = bayesianAPI.doBayesian(input);
        assertNotNull(result);
        assertEquals(0.02, result.results().get(0).probability(), 0.01);
        assertEquals(0.10, result.results().get(0).conversionRate(), 0.00);
        assertEquals(0.89, result.results().get(0).expectedLoss(), 0.01);
        assertNull(result.results().get(0).medianGrowth());
        assertEquals(0.98, result.results().get(1).probability(), 0.01);
        assertEquals(0.13, result.results().get(1).conversionRate(), 0.00);
        assertEquals(0.89, result.results().get(1).expectedLoss(), 0.01);
        assertEquals(0.23, result.results().get(1).medianGrowth(), 0.01);
        assertEquals("test", result.suggestedWinner());
        assertEquals(1000, result.differenceData().controlData().length);
        assertEquals(1000, result.differenceData().testData().length);
        assertEquals(1000, result.differenceData().differences().length);
        assertEquals(0.03, result.differenceData().relativeDifference(), 0.01);
        assertEquals(11, result.quantiles().size());
        assertEquals(2, result.distributionPdfs().samples().size());
        assertEquals(1000, result.distributionPdfs().samples().get("control").size());
        assertEquals(1000, result.distributionPdfs().samples().get("test").size());
        assertTrue(Arrays.stream(BayesianAPI.QUANTILES).allMatch(result.quantiles()::containsKey));
        assertTrue(Arrays.stream(BayesianAPI.QUANTILES).allMatch(key -> Objects.nonNull(result.quantiles().get(key))));
    }

    /**
     * Given Bayesian input parameters with an invalid prior alpha.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorAlpha() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(5).failures(3).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(5).failures(3).isControl(false).build())
            .priors(List.of(BayesianPriors.builder().alpha(0.0).beta(10.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with an invalid prior beta.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorBeta() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(5).failures(3).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(5).failures(3).isControl(false).build())
            .priors(List.of(BayesianPriors.builder().alpha(10.0).beta(0.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with an invalid control successes.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidControlSuccesses() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(-1).failures(3).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(5).failures(3).isControl(false).build())
            .priors(List.of(BayesianPriors.builder().alpha(10.0).beta(10.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with an invalid control failures.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidControlFailures() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(5).failures(-1).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(5).failures(3).isControl(false).build())
            .priors(List.of(BayesianPriors.builder().alpha(10.0).beta(10.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with an invalid test successes.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidTestSuccesses() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(5).failures(3).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(-1).failures(3).isControl(false).build())
            .priors(List.of(BayesianPriors.builder().alpha(10.0).beta(10.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with an invalid test failures.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidTestFailures() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInput.builder().variant("control").successes(5).failures(3).isControl(true).build())
            .addVariantPairs(VariantInput.builder().variant("test").successes(5).failures(-1).isControl(false).build())
            .priors(List.of(BayesianPriors.builder().alpha(10.0).beta(10.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with these values:
     *
     * <pre>
     *     controlSuccesses: 124
     *     controlFailures: 1802
     *     testBSuccesses: 109
     *     testBFailures: 1430
     *     testCSuccesses: 104
     *     testCFailures: 1773
     * </pre>
     *
     * Expect that the probability B beats A and C is at least 0.76.
     */
    @Test
    public void test_doBayesian_ABC() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.ABC)
            .control(VariantInput.builder().variant("control").successes(124).failures(1802).isControl(true).build())
            .addVariantPairs(
                VariantInput.builder().variant("testB").successes(109).failures(1430).isControl(false).build(),
                VariantInput.builder().variant("testC").successes(104).failures(1773).isControl(false).build())
            .priors(List.of(BayesianAPI.DEFAULT_PRIORS, BayesianAPI.DEFAULT_PRIORS, BayesianAPI.DEFAULT_PRIORS))
            .build();
        final BayesianResult result = bayesianAPI.doBayesian(input);
        assertNotNull(result);
        assertEquals(0.21, result.results().get(0).probability(), 0.01);
        assertEquals(0.06, result.results().get(0).conversionRate(), 0.01);
        assertEquals(0.93, result.results().get(0).expectedLoss(), 0.01);
        assertNull(result.results().get(0).medianGrowth());
        assertNull(result.results().get(0).risk());
        assertEquals(0.76, result.results().get(1).probability(), 0.01);
        assertEquals(0.07, result.results().get(1).conversionRate(), 0.01);
        assertEquals(0.96, result.results().get(1).expectedLoss(), 0.01);
        assertEquals(0.09, result.results().get(1).medianGrowth(), 0.01);
        assertEquals(0.015, result.results().get(2).probability(), 0.001);
        assertEquals(0.05, result.results().get(2).conversionRate(), 0.01);
        assertEquals(0.72, result.results().get(2).expectedLoss(), 0.01);
        assertEquals(-0.16, result.results().get(2).medianGrowth(), 0.01);
        assertEquals("testB", result.suggestedWinner());
        assertEquals(3, result.distributionPdfs().samples().size());
        assertEquals(1000, result.distributionPdfs().samples().get("control").size());
        assertEquals(1000, result.distributionPdfs().samples().get("testB").size());
        assertEquals(1000, result.distributionPdfs().samples().get("testC").size());
    }

}
