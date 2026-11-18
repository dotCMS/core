package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Thrown when a concurrent {@code _start} request loses the free-slot advisory lock race:
 * both callers passed {@link ExperimentLimitedModeGate}'s pre-check, but by the time the
 * loser re-checked inside the {@code @WrapInTransaction} boundary, the slot was already
 * occupied by the winner's commit.
 *
 * <p>Callers (e.g. {@code ExperimentsResource.start()}) should treat this identically to a
 * {@code FEATURE_DISABLED} gate rejection — the slot is occupied and the start must not proceed.
 */
public class FreeSlotOccupiedException extends DotRuntimeException {

    public FreeSlotOccupiedException() {
        super("Free experiment slot already in use");
    }
}