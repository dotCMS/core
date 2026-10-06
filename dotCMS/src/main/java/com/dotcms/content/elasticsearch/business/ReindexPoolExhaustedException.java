package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Thrown when the reindex mapping pool cannot accept work: either every concurrent slot is busy,
 * or the abandonment ceiling has been reached and the circuit breaker is open.
 *
 * <p>This is an <strong>infrastructure</strong> failure, not a fault of the journal entry that
 * happened to arrive while the pool was unavailable. Callers must therefore leave the entry's
 * error count and priority untouched and simply put it back — charging it a retry attempt is what
 * turned a stalled queue into a destroyed one in issue #37038.</p>
 */
public class ReindexPoolExhaustedException extends DotRuntimeException {

    private final boolean circuitOpen;

    public ReindexPoolExhaustedException(final String message, final boolean circuitOpen) {
        super(message);
        this.circuitOpen = circuitOpen;
    }

    /**
     * @return {@code true} when the abandonment ceiling was hit — the storage or index endpoint is
     *         very likely down, and the reindex loop should back off rather than keep trying the
     *         next entry. {@code false} means the pool was merely momentarily saturated.
     */
    public boolean isCircuitOpen() {
        return circuitOpen;
    }
}
