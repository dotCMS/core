package com.dotcms.analytics.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Token Status possible values.
 *
 * @author vico
 */
public enum TokenStatus {

    OK,
    IN_WINDOW,
    EXPIRED,
    BLOCKED,
    NOOP,
    NONE;

    public boolean matchesAny(final TokenStatus... statuses) {
        return Optional
            .ofNullable(statuses)
            .map(s -> Arrays.stream(s).anyMatch(status -> status == this))
            .orElse(false);
    }

}
