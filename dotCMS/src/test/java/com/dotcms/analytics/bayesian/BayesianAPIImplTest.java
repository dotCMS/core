package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianPriors;
import com.dotcms.analytics.bayesian.model.BayesianResult;
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
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .controlSuccesses(5)
            .controlFailures(3)
            .testSuccesses(6)
            .testFailures(2)
            .build();
        final BayesianResult result = bayesianAPI.calcProbBOverA(input);
        Assert.assertNotNull(result);
        Assert.assertEquals("0.69", String.format("%.2f", result.value()));
    }

    /**
     * Given Bayesian input parameters with an invalid prior alpha.
     * Expect a IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorAlpha() {
        final BayesianInput input = BayesianInput.builder()
            .priors(BayesianPriors.builder().alpha(0.0).beta(10.0).build())
            .controlSuccesses(5)
            .controlFailures(3)
            .testSuccesses(5)
            .testFailures(3)
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
            .priors(BayesianPriors.builder().alpha(10.0).beta(0.0).build())
            .controlSuccesses(5)
            .controlFailures(3)
            .testSuccesses(5)
            .testFailures(3)
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
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .controlSuccesses(-1)
            .controlFailures(3)
            .testSuccesses(5)
            .testFailures(3)
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
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .controlSuccesses(5)
            .controlFailures(-1)
            .testSuccesses(5)
            .testFailures(3)
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
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .controlSuccesses(5)
            .controlFailures(3)
            .testSuccesses(-1)
            .testFailures(3)
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
            .priors(BayesianPriors.builder().alpha(10.0).beta(10.0).build())
            .controlSuccesses(5)
            .controlFailures(3)
            .testSuccesses(5)
            .testFailures(-1)
            .build();
        bayesianAPI.calcProbBOverA(input);
    }

}
