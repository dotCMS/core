package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.model.ABTestingType;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.bayesian.model.VariantBayesianInput;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * Bayesian API implementation class unit tests.
 *
 * @author vico
 */
public class BayesianAPIImplTest {

    private BayesianAPI bayesianAPI;

    @Before
    public void setup() {
        bayesianAPI = new BayesianAPIImpl();
    }

    /**
     * Given Bayesian input parameters with these values:
     *
     * <pre>
     *     priorAlpha: 2.0
     *     priorBeta: 2.0
     *     controlSuccesses: 150
     *     controlFailures: 50
     *     testSuccesses: 200
     *     testFailures: 50
     * </pre>
     *
     * Expect that the probability B beats A is at least 0.89.
     */
    @Test
    public void test_doBayesian_AB() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantBayesianInput.builder().variant("control").successes(100).failures(900).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(130).failures(870).build())
            .priors(List.of(BayesianPriors.builder().alpha(1.0).beta(1.0).build()))
            .build();
        final BayesianResult result = bayesianAPI.doBayesian(input);
        assertNotNull(result);
        assertEquals(0.02, result.results().get(0).probability(), 0.01);
        assertEquals(0.10, result.results().get(0).conversionRate(), 0.00);
        assertNull(result.results().get(0).medianGrowth());
        assertEquals(0.98, result.results().get(1).probability(), 0.01);
        assertEquals(0.13, result.results().get(1).conversionRate(), 0.00);
        assertEquals(0.23, result.results().get(1).medianGrowth(), 0.01);
        assertEquals("test", result.suggestedWinner());
        assertEquals(1000, result.differenceData().controlData().length);
        assertEquals(1000, result.differenceData().testData().length);
        assertEquals(1000, result.differenceData().differences().length);
        assertEquals(0.03, result.differenceData().relativeDifference(), 0.001);
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
            .control(VariantBayesianInput.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(5).failures(3).build())
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
            .control(VariantBayesianInput.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(5).failures(3).build())
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
            .control(VariantBayesianInput.builder().variant("control").successes(-1).failures(3).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(5).failures(3).build())
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
            .control(VariantBayesianInput.builder().variant("control").successes(5).failures(-1).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(5).failures(3).build())
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
            .control(VariantBayesianInput.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(-1).failures(3).build())
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
            .control(VariantBayesianInput.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantBayesianInput.builder().variant("test").successes(5).failures(-1).build())
            .priors(List.of(BayesianPriors.builder().alpha(10.0).beta(10.0).build()))
            .build();
        bayesianAPI.doBayesian(input);
    }

    /**
     * Given Bayesian input parameters with these values:
     *
     * <pre>
     *     priorAlpha: 10
     *     priorBeta: 10
     *     controlSuccesses: 5
     *     controlFailures: 3
     *     testBSuccesses: 6
     *     testBFailures: 2
     *     testCSuccesses: 7
     *     testCFailures: 1
     * </pre>
     *
     * Expect that the probability B beats A is at least 0.69.
     */
    @Test
    public void test_calculateABCTesting() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.ABC)
            .control(VariantBayesianInput.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(
                VariantBayesianInput.builder().variant("testB").successes(6).failures(2).build(),
                VariantBayesianInput.builder().variant("testC").successes(7).failures(1).build())
            .priors(List.of(BayesianAPI.DEFAULT_PRIORS, BayesianAPI.DEFAULT_PRIORS, BayesianAPI.DEFAULT_PRIORS))
            .build();
        final BayesianResult result = bayesianAPI.doBayesian(input);
        assertNotNull(result);
        assertEquals(0.08, result.results().get(0).probability(), 0.01);
        assertEquals(0.25, result.results().get(1).probability(), 0.01);
        assertEquals(0.65, result.results().get(2).probability(), 0.01);
        assertEquals("testC", result.suggestedWinner());
    }

}
