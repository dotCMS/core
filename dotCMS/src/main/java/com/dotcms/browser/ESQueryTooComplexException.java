package com.dotcms.browser;

import com.dotmarketing.exception.DotDataException;

/**
 * The index rejected a Content Drive sub-query because the query built from the user's search
 * term or filter values was too complex for it to build: a wildcard it cannot determinize, or
 * too many boolean clauses. The input is at fault, not the index, so Content Drive answers
 * HTTP 400 for it while every other index failure fails the request as a server error
 * (issue #37488).
 */
public class ESQueryTooComplexException extends DotDataException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message What was rejected, for the log.
     * @param cause   The failure the index raised.
     */
    public ESQueryTooComplexException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
