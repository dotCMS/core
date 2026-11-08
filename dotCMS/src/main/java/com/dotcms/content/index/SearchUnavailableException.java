package com.dotcms.content.index;

import com.dotmarketing.exception.DotRuntimeException;

/** A temporary search failure, never a successful empty result or a cacheable response. */
public final class SearchUnavailableException extends DotRuntimeException {

    /** Low-cardinality reasons used by search admission metrics. */
    public enum Reason { REJECTED, DEADLINE, INTERRUPTED, BACKEND }

    private static final long serialVersionUID = 1L;
    private final Reason reason;

    /** Creates a failure with a public-safe message and an optional diagnostic cause. */
    public SearchUnavailableException(final Reason reason, final Throwable cause) {
        super("Search temporarily unavailable. Please retry later.", cause);
        this.reason = reason;
    }

    /** Returns the reason without exposing the query or transport configuration. */
    public Reason reason() {
        return reason;
    }

    /** Finds temporary search failures through the wrappers used by Velocity and content APIs. */
    public static SearchUnavailableException find(final Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 64; depth++) {
            if (current instanceof SearchUnavailableException) {
                return (SearchUnavailableException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    /** Prevents legacy catch-and-return-empty handlers from absorbing a temporary outage. */
    public static void rethrow(final Throwable failure) {
        final SearchUnavailableException unavailable = find(failure);
        if (unavailable != null) {
            throw unavailable;
        }
    }
}
