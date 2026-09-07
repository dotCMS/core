package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Thrown when mapping one journal entry overran the configured timeout and its worker thread was
 * abandoned.
 *
 * <p>Unlike {@link ReindexPoolExhaustedException} this <em>is</em> attributable to the entry being
 * mapped — a binary field on unreachable storage, for instance — so the entry is failed normally
 * and charged a retry attempt.</p>
 */
public class ReindexMappingTimeoutException extends DotRuntimeException {

    public ReindexMappingTimeoutException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
