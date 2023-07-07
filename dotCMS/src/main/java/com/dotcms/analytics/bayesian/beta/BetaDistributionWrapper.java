package com.dotcms.analytics.bayesian.beta;

import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.stream.Stream;

/**
 * Customized Beta Distribution class wrapping utilities found in Apache's Common Math and Apache's Commons Numbers
 * libraries.
 *
 * @author vico
 */
public class BetaDistributionWrapper {

    private final BetaDistribution betaDistribution;

    /**
     * Instantiates class with alpha and beta parameters.
     *
     * @param alpha alpha value
     * @param beta beta value
     * @param betaInverse beta inverse value
     */
    private BetaDistributionWrapper(final double alpha, final double beta, final Double betaInverse) {
        betaDistribution = betaInverse != Double.NEGATIVE_INFINITY
            ? new BetaDistribution(alpha, beta, betaInverse)
            : new BetaDistribution(alpha, beta);
    }

    /**
     * Creates new instance of {@link BetaDistributionWrapper}.
     *
     * @param alpha alpha value
     * @param beta beta value
     * @return a new {@link BetaDistributionWrapper} instance
     */
    public static BetaDistributionWrapper create(final double alpha, final double beta, final double betaInverse) {
        return new BetaDistributionWrapper(alpha, beta, betaInverse);
    }

    /**
     * Creates new instance of {@link BetaDistributionWrapper}.
     *
     * @param alpha alpha value
     * @param beta beta value
     * @return a new {@link BetaDistributionWrapper} instance
     */
    public static BetaDistributionWrapper create(final double alpha, final double beta) {
        return new BetaDistributionWrapper(alpha, beta, Double.NEGATIVE_INFINITY);
    }

    /**
     * Returns log density actual result.
     *
     * @param x number to calculate density from
     * @return log density result
     */
    public double lp(final double x) {
        return betaDistribution.logDensity(x);
    }

    /**
     * Returns density actual result.
     *
     * @param x number to calculate density from
     * @return density result
     */
    public double pdf(final double x) {
        return betaDistribution.density(x);
    }

    /**
     * Creates a distribution sample.
     *d
     * @return double sample
     */
    public double rv() {
        return betaDistribution.sample();
    }

    /**
     * Creates a limited (by provided size) generation of samples.
     *
     * @param size size
     * @return list of samples
     */
    public double[] rvs(final long size) {
        return Stream.generate(this::rv).limit(size).mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Returns inverse cumulative probability actual result.
     *
     * @param x number to calculate cumulative probability from
     * @return cumulative probability result
     */
    public double inverseCumulativeProbability(final double x) {
        return betaDistribution.inverseCumulativeProbability(x);
    }

    @Override
    public String toString() {
        return "BetaDistributionWrapper{" +
            "betaDistribution=" + betaDistribution +
            '}';
    }
}
