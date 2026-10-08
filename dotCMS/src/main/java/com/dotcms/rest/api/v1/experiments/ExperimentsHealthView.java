package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response shape for {@code GET /api/v1/experiments/health}.
 *
 * <p>Replaces the previous {@code Map<String, Health>} return type. Carries the existing health
 * state plus the new tier fields that drive portlet button state in the frontend.
 *
 * <p>Field presence rules:
 * <ul>
 *   <li>{@link #health} — always present.
 *   <li>{@link #tier} — always present; {@link Tier#FULL} when {@code FEATURE_FLAG_EXPERIMENTS=true},
 *       {@link Tier#LIMITED} when {@code false}. App configuration has no effect on this field.
 *   <li>{@link #freeExperimentUsed} — present (non-null) only when {@code tier=LIMITED};
 *       {@code null} when {@code tier=FULL} because the slot concept does not apply.
 *   <li>{@link #warning} — present only when the Analytics App is not configured for the site;
 *       absent otherwise. Fixed value {@code "ANALYTICS_DISABLED"}.
 * </ul>
 *
 * @author dotCMS
 * @since Oct 2026
 */
@JsonInclude(Include.NON_NULL)
public class ExperimentsHealthView {

    /** Serialized as {@code "FULL"} or {@code "LIMITED"} in the JSON response. */
    public enum Tier {
        FULL, LIMITED
    }

    /** Present when the Analytics App is not configured for the site; absent otherwise. */
    public enum Warning {
        ANALYTICS_DISABLED
    }

    @Schema(description = "Current health state of the experiments / analytics configuration")
    private final Health health;

    @Schema(description = "License tier: FULL when experiments are fully enabled, LIMITED when restricted to one free experiment")
    private final Tier tier;

    @Schema(description = "Whether the single free experiment slot is already occupied. Present only when tier=LIMITED; null when tier=FULL.")
    private final Boolean freeExperimentUsed;

    @Schema(description = "Warning code present when the Analytics App is not configured for the site. Absent when configured.")
    private final Warning warning;

    private ExperimentsHealthView(final Builder builder) {
        this.health = builder.health;
        this.tier = builder.tier;
        this.freeExperimentUsed = builder.freeExperimentUsed;
        this.warning = builder.warning;
    }

    public Health getHealth() {
        return health;
    }

    public Tier getTier() {
        return tier;
    }

    public Boolean getFreeExperimentUsed() {
        return freeExperimentUsed;
    }

    public Warning getWarning() {
        return warning;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Health health;
        private Tier tier;
        private Boolean freeExperimentUsed;
        private Warning warning;

        private Builder() {
        }

        public Builder health(final Health health) {
            this.health = health;
            return this;
        }

        public Builder tier(final Tier tier) {
            this.tier = tier;
            return this;
        }

        public Builder freeExperimentUsed(final Boolean freeExperimentUsed) {
            this.freeExperimentUsed = freeExperimentUsed;
            return this;
        }

        public Builder warning(final Warning warning) {
            this.warning = warning;
            return this;
        }

        public ExperimentsHealthView build() {
            return new ExperimentsHealthView(this);
        }
    }
}