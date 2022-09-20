package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;


/**
 * Bayesian inference calculation input. It consists in 6 known parameters:
 *  <ul>
 *      <li>prior alpha: prior data for alpha</li>
 *      <li>prior beta: prior data for beta </li>
 *      <li>control successes: number of successes for control (A)</li>
 *      <li><control failures: number of failures for control (A)/li>
 *      <li>test successes: number of </li>
 *      <li>test failures:</li>
 *  </ul>
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractBayesianInput {

    @JsonProperty("priorAlpha")
    double priorAlpha();

    @JsonProperty("priorBeta")
    double priorBeta();

    @JsonProperty("controlSuccesses")
    int controlSuccesses();

    @JsonProperty("controlFailures")
    int controlFailures();

    @JsonProperty("testSuccesses")
    int testSuccesses();

    @JsonProperty("testFailures")
    int testFailures();

}
