package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Evaluates the limited-mode constraints for starting an {@link Experiment}.
 *
 * <p>When {@code FEATURE_FLAG_EXPERIMENTS=false}, only a single immediate experiment within the
 * {@value MAX_DAYS}-day duration cap is allowed. This class centralises those checks so callers
 * stay focused on their own responsibilities.
 *
 * <p>{@link #evaluate} is a pure check — it does not mutate state. The caller is responsible
 * for any side-effects (e.g. auto-setting the end date via {@link #defaultEndDate}).
 *
 * @author dotCMS
 * @since Oct 2026
 */
public class ExperimentLimitedModeGate {

    /** Maximum duration allowed for a free experiment in limited mode, in days. */
    public static final long MAX_DAYS = 10L;

    /**
     * Reason why the limited-mode gate blocked a start request. The caller maps each value
     * to the appropriate response.
     */
    public enum Rejection {
        /** Scheduling or slot constraints prevent this start. */
        FEATURE_DISABLED,
        /** The experiment has a future startDate, which is not allowed in limited mode. */
        SCHEDULED_START_NOT_ALLOWED,
        /** The experiment's duration exceeds the cap. */
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
     * @throws DoesNotExistException if the experiment does not exist (unchecked, maps to 404)
     * @throws DotDataException      on persistence errors from {@code find} or {@code isFreeSlotUsed}
     * @throws DotSecurityException on permission errors
     */
    public static Optional<Rejection> evaluate(final String experimentId, final User user)
            throws DotDataException, DotSecurityException {

        final ExperimentsAPI experimentsAPI = APILocator.getExperimentsAPI();
        final Experiment experiment = experimentsAPI.find(experimentId, user)
                .orElseThrow(() -> new DoesNotExistException(
                        "Experiment not found: " + experimentId));

        final Optional<Scheduling> schedulingOpt = experiment.scheduling();

        // Scheduled (future-dated) starts are not allowed in limited mode
        if (schedulingOpt.isPresent()
                && schedulingOpt.get().startDate().isPresent()
                && schedulingOpt.get().startDate().get().isAfter(Instant.now())) {
            return Optional.of(Rejection.SCHEDULED_START_NOT_ALLOWED);
        }

        // Only one free experiment slot is allowed system-wide
        if (experimentsAPI.isFreeSlotUsed()) {
            return Optional.of(Rejection.FEATURE_DISABLED);
        }

        // Duration cap: end date must not exceed exactly MAX_DAYS from now.
        // isAfter() compares full instants — avoids the off-by-one that DAYS.between()
        // introduces by truncating sub-day remainders (e.g. "10d 23h" would count as 10).
        final Optional<Instant> endDateOpt = schedulingOpt.flatMap(Scheduling::endDate);
        if (endDateOpt.isPresent()
                && endDateOpt.get().isAfter(Instant.now().plus(MAX_DAYS, ChronoUnit.DAYS))) {
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
    public static Instant defaultEndDate(final Instant effectiveStart) {
        return effectiveStart.plus(MAX_DAYS, ChronoUnit.DAYS);
    }
}