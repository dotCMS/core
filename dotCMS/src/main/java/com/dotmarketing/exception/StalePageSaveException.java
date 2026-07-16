package com.dotmarketing.exception;

/**
 * Thrown when an empty-payload save would wipe existing page content, indicating the caller's
 * session is stale (another user has added content since the session was opened).
 * Handled as HTTP 409 Conflict in PageResource — callers should prompt the user to refresh.
 */
public class StalePageSaveException extends DotDataException {

    public StalePageSaveException(final String message) {
        super(message);
    }
}
