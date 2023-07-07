package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Variant successes and failures pair.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = VariantResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractVariantResult {

    @JsonProperty("variant")
    String variant();

    @JsonProperty("isControl")
    boolean isControl();

    @JsonProperty("conversionRate")
    double conversionRate();

    @JsonProperty("probability")
    double probability();

    @Nullable
    @JsonProperty("expectedLoss")
    Double expectedLoss();

    @Nullable
    @JsonProperty("risk")
    Double risk();

    @Nullable
    @JsonProperty("medianGrowth")
    Double medianGrowth();

    @Nullable
    @JsonProperty("credibilityInterval")
    CredibilityInterval credibilityInterval();

}
