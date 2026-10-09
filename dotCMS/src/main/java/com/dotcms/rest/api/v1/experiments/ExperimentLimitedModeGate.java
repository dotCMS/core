package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Evaluates the limited-mode constraints for {@code POST /{id}/_start}.
 *
 * <p>When {@code FEATURE_FLAG_EXPERIMENTS=false}, only a single immediate experiment within the
 * {@value MAX_DAYS}-day duration cap is allowed. This class centralises those checks so
 * {@link ExperimentsResource#start} stays focused on request handling.
 *
 * <p>{@link #evaluate} is a pure check — it does not mutate state. The caller is responsible
 * for any side-effects (e.g. auto-setting the end date via {@link #defaultEndDate}).
 *
 * @author dotCMS
 * @since Oct 2026
 */
class ExperimentLimitedModeGate {

    /**
     * Maximum duration allowed for a free experiment in limited mode, in days.
     * Delegates to {@link ExperimentsAPI#LIMITED_MODE_MAX_DAYS} — single source of truth.
     */
    static final long MAX_DAYS = ExperimentsAPI.LIMITED_MODE_MAX_DAYS;

    /**
     * Reason why the limited-mode gate blocked a start request. The caller maps each value
     * to the appropriate HTTP response.
     */
    enum Rejection {
        /** Scheduling or slot constraints prevent this start. Returns {@code 403}. */
        FEATURE_DISABLED,
        /** The experiment's duration exceeds the cap. Returns {@code 400}. */
        DURATION_EXCEEDED
    }

    private ExperimentLimitedModeGate() {
    }

    /**
     * Checks all limited-mode constraints for a start request. Pure — does not mutate state.
     *
     * @param experimentId the experiment being started
     * @param user         the authenticated caller
     * @return empty if the start is allowed; the {@link Rejection} reason if it must be blocked
     * @throws NotFoundException    if the experiment does not exist
     * @throws DotDataException     on persistence errors
     * @throws DotSecurityException on permission errors
     */
    static Optional<Rejection> evaluate(final String experimentId, final User user)
            throws DotDataException, DotSecurityException {

        final ExperimentsAPI experimentsAPI = APILocator.getExperimentsAPI();
        final Experiment experiment = experimentsAPI.find(experimentId, user)
                .orElseThrow(() -> new NotFoundException(
                        "Experiment not found: " + experimentId));

        final Optional<Scheduling> schedulingOpt = experiment.scheduling();

        // Scheduled (future-dated) starts are not allowed in limited mode
        if (schedulingOpt.isPresent()
                && schedulingOpt.get().startDate().isPresent()
                && schedulingOpt.get().startDate().get().isAfter(Instant.now())) {
            return Optional.of(Rejection.FEATURE_DISABLED);
        }

        // Only one free experiment slot is allowed system-wide
        if (experimentsAPI.isFreeSlotUsed()) {
            return Optional.of(Rejection.FEATURE_DISABLED);
        }

        // Duration cap: if an end date is set, it must not exceed MAX_DAYS from now
        final Optional<Instant> endDateOpt = schedulingOpt.flatMap(Scheduling::endDate);
        if (endDateOpt.isPresent()
                && ChronoUnit.DAYS.between(Instant.now(), endDateOpt.get()) > MAX_DAYS) {
            return Optional.of(Rejection.DURATION_EXCEEDED);
        }

        return Optional.empty();
    }

    /**
     * Returns the default end date for a free experiment: {@value MAX_DAYS} days from the
     * given effective start. Used by the caller to auto-set the end date when none is provided.
     *
     * @param effectiveStart the instant the experiment will start (typically {@code Instant.now()})
     * @return the computed end date at the duration cap
     */
    static Instant defaultEndDate(final Instant effectiveStart) {
        return effectiveStart.plus(MAX_DAYS, ChronoUnit.DAYS);
    }
}