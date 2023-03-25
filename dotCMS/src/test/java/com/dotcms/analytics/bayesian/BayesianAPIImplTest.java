package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.model.ABTestingType;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import com.dotcms.analytics.bayesian.model.VariantInputPair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


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
     *     priorAlpha: 10
     *     priorBeta: 10
     *     controlSuccesses: 5
     *     controlFailures: 3
     *     testSuccesses: 6
     *     testFailures: 2
     * </pre>
     *
     * Expect that the probability B beats A is at least 0.69.
     */
    @Test
    public void test_calculateABTesting() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(6).failures(2).build())
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .build();
        final BayesianResult result = bayesianAPI.calcProbBOverA(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(0.31, result.probabilities().get(0).value(), 0.01);
        Assert.assertEquals(0.69, result.probabilities().get(1).value(), 0.01);
        Assert.assertEquals("test", result.suggested());
    }

    /**
     * Given Bayesian input parameters with an invalid prior alpha.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorAlpha() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(5).failures(3).build())
            .priors(BayesianPriors.builder().alpha(0.0).beta(10.0).build())
            .build();
        bayesianAPI.calcProbBOverA(input);
    }

    /**
     * Given Bayesian input parameters with an invalid prior beta.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorBeta() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(5).failures(3).build())
            .priors(BayesianPriors.builder().alpha(10.0).beta(0.0).build())
            .build();
        bayesianAPI.calcProbBOverA(input);
    }

    /**
     * Given Bayesian input parameters with an invalid control successes.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidControlSuccesses() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(-1).failures(3).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(5).failures(3).build())
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .build();
        bayesianAPI.calcProbBOverA(input);
    }

    /**
     * Given Bayesian input parameters with an invalid control failures.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidControlFailures() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(5).failures(-1).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(5).failures(3).build())
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .build();
        bayesianAPI.calcProbBOverA(input);
    }

    /**
     * Given Bayesian input parameters with an invalid test successes.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidTestSuccesses() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(-1).failures(3).build())
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .build();
        bayesianAPI.calcProbBOverA(input);
    }

    /**
     * Given Bayesian input parameters with an invalid test failures.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidTestFailures() {
        final BayesianInput input = BayesianInput.builder()
            .type(ABTestingType.AB)
            .control(VariantInputPair.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(VariantInputPair.builder().variant("test").successes(5).failures(-1).build())
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .build();
        bayesianAPI.calcProbBOverA(input);
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
            .control(VariantInputPair.builder().variant("control").successes(5).failures(3).build())
            .addVariantPairs(
                VariantInputPair.builder().variant("testB").successes(6).failures(2).build(),
                VariantInputPair.builder().variant("testC").successes(7).failures(1).build())
            .priors(BayesianAPI.NULL_PRIORS)
            .build();
        final BayesianResult result = bayesianAPI.calcProbABC(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(0.08, result.probabilities().get(0).value(), 0.01);
        Assert.assertEquals(0.25, result.probabilities().get(1).value(), 0.01);
        Assert.assertEquals(0.65, result.probabilities().get(2).value(), 0.01);
        Assert.assertEquals("testC", result.suggested());
    }

}
