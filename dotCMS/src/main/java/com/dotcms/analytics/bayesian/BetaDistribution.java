package com.dotcms.analytics.bayesian;

import org.apache.commons.numbers.gamma.LogGamma;

import java.util.function.Function;
import java.util.stream.Stream;


/**
 * Customized Beta Distribution class wrapping utilities found in Apache's Common Math and Apache's Commons Numbers
 * libraries.
 *
 * @author vico
 */
public class BetaDistribution {

    private final double alpha;
    private final double beta;
    // do we even need to set this for Commons Math ?
    private final double betaInverse;

    /**
     * Instantiates class with alpha and beta parameters.
     *
     * @param alpha alpha value
     * @param beta beta value
     */
    private BetaDistribution(final double alpha, final double beta) {
        this.alpha = alpha;
        this.beta = beta;
        // do we need to set this for Commons Math ?
        betaInverse = LogGamma.value(this.alpha + this.beta) - LogGamma.value(this.alpha) - LogGamma.value(this.beta);
    }

    /**
     * Creates new instance of {@link BetaDistribution}.
     *
     * @param alpha alpha value
     * @param beta beta value
     * @return a new {@link BetaDistribution} instance
     */
    public static BetaDistribution create(final double alpha, final double beta) {
        return new BetaDistribution(alpha, beta);
    }

    /**
     * Creates a function that returns the log density for a provided number.
     *
     * @return {@link Function} instance
     */
    public Function<Double, Double> lp() {
        return x -> distribution().logDensity(x);
    }

    /**
     * Returns log density actual result.
     *
     * @param x number to calculate density from
     * @return log density result
     */
    public double lp(final double x) {
        return lp().apply(x);
    }

    /**
     * Creates a function that returns the density for a provided number.
     *
     * @return {@link Function} instance
     */
    public Function<Double, Double> pdf() {
        return x -> distribution().density(x);
    }

    /**
     * Returns density actual result.
     *
     * @param x number to calculate density from
     * @return density result
     */
    public double pdf(final double x) {
        return pdf().apply(x);
    }

    /**
     * Creates a distribution sample.
     *
     * @return double sample
     */
    public double rv() {
        return distribution().sample();
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
     * Creates a {@link org.apache.commons.math3.distribution.BetaDistribution} instance based on current instance's alpha, beta and inverse beta values.
     *
     * @return Apache's Commons Math beta distribution instance.
     */
    private org.apache.commons.math3.distribution.BetaDistribution distribution() {
        return new org.apache.commons.math3.distribution.BetaDistribution(alpha, beta, betaInverse);
    }

    @Override
    public String toString() {
        return "DotBetaDistribution{" +
                "alpha=" + alpha +
                ", beta=" + beta +
                ", betaInverse=" + betaInverse +
                '}';
    }

}
