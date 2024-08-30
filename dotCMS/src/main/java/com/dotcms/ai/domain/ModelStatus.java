package com.dotcms.ai.domain;

/**
 * Represents the status of an AI model.
 *
 * <p>
 * This enum defines various statuses that an AI model can have, such as active, invalid, decommissioned, or unknown.
 * Each status may have different implications for the operation of the model.
 * </p>
 *
 * @author vico
 */
public enum ModelStatus {

    ACTIVE(false),
    INVALID(false),
    DECOMMISSIONED(false),
    UNKNOWN(true);

    private final boolean needsToThrow;

    ModelStatus(final boolean needsToThrow) {
        this.needsToThrow = needsToThrow;
    }

    public boolean doesNeedToThrow() {
        return needsToThrow;
    }

}
