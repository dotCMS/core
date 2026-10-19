package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response shape for {@code GET /api/v1/experiments/health}.
 *
 * <p>Carries the health state plus the tier fields that drive portlet button state in the
 * frontend. {@link JsonInclude#NON_NULL} ensures absent optional fields are omitted from the
 * JSON response rather than written as {@code null}.
 *
 * <p>Field presence rules:
 * <ul>
 *   <li>{@link #health} — always present.
 *   <li>{@link #tier} — always present; {@link Tier#FULL} when {@code FEATURE_FLAG_EXPERIMENTS=true},
 *       {@link Tier#LIMITED} when {@code false}. App configuration has no effect on this field.
 *   <li>{@link #freeExperimentUsed} — present (non-null) only when {@code tier="limited"};
 *       {@code null} when {@code tier="full"} because the slot concept does not apply.
 *   <li>{@link #warning} — present only when the Analytics App is not configured for the site;
 *       absent otherwise. Fixed value {@code "analytics_disabled"}.
 * </ul>
 *
 * @author dotCMS
 * @since Oct 2026
 */
@JsonInclude(Include.NON_NULL)
public record ExperimentsHealthView(

        @Schema(description = "Current health state of the experiments / analytics configuration")
        Health health,

        @Schema(description = "License tier: \"full\" when experiments are fully enabled, \"limited\" when restricted to one free experiment")
        Tier tier,

        @Schema(description = "Whether the single free experiment slot is already occupied. Present only when tier=\"limited\"; null when tier=\"full\".")
        Boolean freeExperimentUsed,

        @Schema(description = "Warning code present when the Analytics App is not configured for the site. Absent when configured.")
        Warning warning

) {

    /**
     * Wire values: {@code "full"} and {@code "limited"} — annotated with {@link JsonProperty}
     * so Jackson (serialization) and swagger-maven-plugin (schema introspection) both see
     * the same lowercase values without duplicates.
     */
    public enum Tier {
        @JsonProperty("full") FULL,
        @JsonProperty("limited") LIMITED
    }

    /**
     * Wire value: {@code "analytics_disabled"} — annotated with {@link JsonProperty}
     * so Jackson and swagger agree. Present when App not configured; absent otherwise.
     */
    public enum Warning {
        @JsonProperty("analytics_disabled") ANALYTICS_DISABLED
    }
}